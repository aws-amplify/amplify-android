#!/usr/bin/env python3
# Copyright 2026 Amazon.com, Inc. or its affiliates. All Rights Reserved.
# SPDX-License-Identifier: Apache-2.0
"""
Publishing-scope containment checker.

Asserts, for one published module, that every EXTERNAL type appearing in the
module's public API surface (its binary-validator .api dump) is present on the
compile classpath that a consumer assembles from the module's PUBLISHED artifact
plus its `api`-scoped transitive closure.

Any public-API type that is NOT on that consumer compile classpath is a scoping
bug: a dependency that supplies a public-API type is scoped `implementation`
(runtime) instead of `api` (compile), so downstream consumers cannot compile
against that part of the module's API.

This script is the pure set-logic half. A Gradle task resolves the consumer
compile classpath (aar/jar files) and invokes this script per module with:
    --api-file    <module>/api/<module>.api
    --classpath   file listing one resolved artifact path per line
    --module      human-readable module name (for reporting)

Exit code 0 = all public-API types resolvable; 1 = violations found.
"""
import argparse
import re
import sys
import zipfile

# Package roots that never need declaring as `api` because they are always present on a
# consumer's compile classpath without any Amplify dependency supplying them:
#  - java/*        JDK, always on every classpath
#  - kotlin/*      Kotlin stdlib, always `api` via the Kotlin plugin
#  - android/*     Android framework, provided by the platform android.jar (never published)
#  - org/json/*    bundled INTO android.jar by the platform (JSONObject etc.) — same as android/*
#  - org/w3c/, org/xml/, javax/xml/  also bundled into android.jar
# These are matched as path prefixes against '/'-separated binary names.
IGNORED_PREFIXES = (
    "java/", "kotlin/", "android/",
    "org/json/", "org/w3c/", "org/xml/", "javax/xml/",
)

# Match every `Lpackage/sub/Class;` type token in a .api file (member descriptors)
TYPE_TOKEN = re.compile(r"L([a-zA-Z][a-zA-Z0-9/$_]*);")
# Match class declaration lines to capture supertypes/interfaces after the ':'
CLASS_DECL = re.compile(r"^(?:public|protected).*?class\s+([a-zA-Z0-9/$_]+)(?:\s*:\s*(.*?))?\s*\{?\s*$")


def normalize(binary_name):
    """Reduce a JVM binary name to its outer class, '/'-separated, no '$' nesting.
    A nested type is available iff its enclosing top-level class is on the classpath,
    so we compare at outer-class granularity."""
    return binary_name.split("$", 1)[0]


def types_in_api(api_path):
    """Return the set of external outer-class binary names referenced in the .api file,
    and the set of types this module DEFINES (to exclude self-references)."""
    referenced = set()
    defined = set()
    with open(api_path, encoding="utf-8") as f:
        for line in f:
            m = CLASS_DECL.match(line.strip())
            if m:
                defined.add(normalize(m.group(1)))
                # supertype + interfaces listed after ':' are also references. They are
                # comma- and/or whitespace-separated, e.g. ":  A, B {" — split on both.
                if m.group(2):
                    for tok in re.split(r"[,\s]+", m.group(2)):
                        tok = tok.strip().rstrip("{").strip()
                        if tok and "/" in tok:
                            referenced.add(normalize(tok))
            for tok in TYPE_TOKEN.findall(line):
                referenced.add(normalize(tok))
    external = {
        t for t in referenced
        if not t.startswith(IGNORED_PREFIXES) and t not in defined
    }
    return external, defined


def classes_on_classpath(classpath_listing):
    """Return the set of outer-class binary names available across all artifacts.
    Handles jars directly and aars (classes live in classes.jar inside the aar)."""
    available = set()
    with open(classpath_listing, encoding="utf-8") as f:
        artifacts = [ln.strip() for ln in f if ln.strip()]
    for art in artifacts:
        try:
            if art.endswith(".jar"):
                _collect_jar_classes(art, available)
            elif art.endswith(".aar"):
                _collect_aar_classes(art, available)
        except (zipfile.BadZipFile, FileNotFoundError, KeyError) as e:
            print(f"  warning: could not read artifact {art}: {e}", file=sys.stderr)
    return available


def _collect_jar_classes(jar_path, out):
    with zipfile.ZipFile(jar_path) as z:
        for name in z.namelist():
            if name.endswith(".class"):
                out.add(normalize(name[:-len(".class")]))


def _collect_aar_classes(aar_path, out):
    with zipfile.ZipFile(aar_path) as aar:
        for entry in aar.namelist():
            # aar bundles compiled code in classes.jar (and occasionally libs/*.jar)
            if entry == "classes.jar" or (entry.startswith("libs/") and entry.endswith(".jar")):
                with aar.open(entry) as jar_stream:
                    with zipfile.ZipFile(jar_stream) as z:
                        for name in z.namelist():
                            if name.endswith(".class"):
                                out.add(normalize(name[:-len(".class")]))


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--api-file", required=True)
    ap.add_argument("--classpath", required=True, help="file listing resolved artifact paths")
    ap.add_argument("--module", required=True)
    args = ap.parse_args()

    required, _ = types_in_api(args.api_file)
    available = classes_on_classpath(args.classpath)

    missing = sorted(t for t in required if t not in available)

    if missing:
        print(f"FAIL [{args.module}]: {len(missing)} public-API type(s) not on consumer compile classpath:")
        for t in missing:
            print(f"    {t.replace('/', '.')}")
        print(f"  -> a dependency supplying these is scoped `implementation` but must be `api`.")
        return 1
    print(f"OK   [{args.module}]: all {len(required)} external public-API types resolvable by consumers.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
