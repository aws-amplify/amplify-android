#!/usr/bin/env bash
# Copyright 2026 Amazon.com, Inc. or its affiliates. All Rights Reserved.
# SPDX-License-Identifier: Apache-2.0
#
# Publishing-scope regression gate.
#
# For every published module, verifies that every external type in the module's public API
# surface (its binary-validator .api dump) is resolvable on the compile classpath a downstream
# consumer assembles from the PUBLISHED artifact. A failure means a dependency supplying a
# public-API type is scoped `implementation` (runtime) but must be `api` (compile) — otherwise
# consumers cannot compile against that part of the module's API.
#
# Pipeline:
#   1. publish all modules to mavenLocal under an isolated version
#   2. ask each module for its authoritative published coordinates (printPublishedCoordinates)
#   3. per coordinate: resolve a synthetic consumer's release compile classpath
#   4. per module: run verify_api_scopes.py (.api surface  ⊆  consumer compile classpath)
#
# Usage: scripts/verify_api_scopes.sh
# Exit: 0 = all modules OK; 1 = at least one scoping violation.
set -euo pipefail

REPO="$(cd "$(dirname "$0")/.." && pwd)"
CONSUMER="$REPO/scripts/scope-check-consumer"
PYCHECK="$REPO/scripts/verify_api_scopes.py"
VER="0.0.0-scopecheck"           # isolated version so nothing shadows via project substitution
WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT

# Plugin versions for the standalone consumer, sourced from the catalog so they never drift.
AGP=$(grep -E '^agp = ' "$REPO/gradle/libs.versions.toml" | head -1 | sed -E 's/.*"([^"]+)".*/\1/')
KGP=$(grep -E '^kotlin = ' "$REPO/gradle/libs.versions.toml" | head -1 | sed -E 's/.*"([^"]+)".*/\1/')

echo "== Publishing all modules to mavenLocal (version $VER) =="
# Note: modules that override VERSION_NAME (apollo/appsync) publish under their own version;
# printPublishedCoordinates reports whatever they actually publish, so both are handled.
(cd "$REPO" && ./gradlew publishToMavenLocal -PVERSION_NAME="$VER" -x signMavenPublication --quiet)

echo "== Discovering published coordinates =="
# One authoritative coordinate list, straight from the publications. Format: group:artifact:version
# Map each artifactId back to its .api file via a second query pairing project path + api dir.
COORDS_FILE="$WORK/coords.txt"
: > "$COORDS_FILE"

# Collect (projectPath -> coordinate) by running the task across all projects at once.
# Must pass the SAME -PVERSION_NAME override used when publishing above: otherwise this reports
# the real release version (e.g. 2.39.0) while the artifacts were published under $VER, so the
# consumer would resolve released Central artifacts instead of the freshly-built ones — silently
# validating the wrong bits for released modules and producing a false failure for any module not
# yet released at that version. Modules that hard-override their version (apollo/appsync) still
# report their own coordinate, since the override wins over the property.
(cd "$REPO" && ./gradlew -q printPublishedCoordinates -PVERSION_NAME="$VER" 2>/dev/null) \
  | grep -E '^[a-z0-9.]+:[a-z0-9-]+:' | sort -u > "$COORDS_FILE" || true

if [ ! -s "$COORDS_FILE" ]; then
  echo "ERROR: no published coordinates discovered" >&2
  exit 2
fi

# Locate the <name>.api file for an artifactId (exactly one per module, outside build dirs).
# KMP modules publish a "-android" coordinate carrying the same classes as the base module, so
# fall back to the base name's .api for those. Kept as a function to stay Bash 3.2 compatible
# (macOS ships 3.2, which lacks associative arrays).
find_api_file() {
  local artifact="$1" f
  f=$(find "$REPO" -type f -path '*/api/'"$artifact"'.api' ! -path '*/build/*' | head -1)
  if [ -z "$f" ] && [ "${artifact%-android}" != "$artifact" ]; then
    f=$(find "$REPO" -type f -path '*/api/'"${artifact%-android}"'.api' ! -path '*/build/*' | head -1)
  fi
  printf '%s' "$f"
}

PASS=0; FAIL=0; SKIP=0; FAILED=""
while IFS= read -r coord; do
  group="${coord%%:*}"; rest="${coord#*:}"; artifact="${rest%%:*}"; version="${rest##*:}"

  api="$(find_api_file "$artifact")"
  if [ -z "$api" ]; then
    echo "SKIP [$coord]: no matching .api file"; SKIP=$((SKIP+1)); continue
  fi

  # Drive the standalone consumer build with the repo's Gradle wrapper via -p (the consumer dir
  # has no wrapper of its own, and this guarantees the same Gradle version as the main build).
  if ! "$REPO/gradlew" -p "$CONSUMER" dumpCompileClasspath \
        -PmoduleCoords="$coord" -Pagp="$AGP" -Pkgp="$KGP" \
        --rerun-tasks --quiet > "$WORK/resolve-$artifact.log" 2>&1; then
    echo "SKIP [$coord]: consumer failed to resolve (see $WORK/resolve-$artifact.log)"
    SKIP=$((SKIP+1)); continue
  fi

  # Guard against the consumer's lenient artifactView silently swallowing an unresolved target
  # module: if the target isn't actually on the resolved classpath (e.g. a brand-new module not
  # yet published, or a skipped publish), the classpath comes back near-empty and the type check
  # would false-FAIL on EVERY public-API type. Require the target's own coordinate to appear on
  # the classpath before trusting the containment check. group and version are taken from $coord,
  # so version-overriding modules (apollo/appsync publish under their own group/version) are
  # handled. The artifact segment is left open ([^/]+) because KMP root coords resolve to their
  # `-android` sibling (foundation -> foundation-android). Both path layouts are matched: the
  # Gradle module cache uses a dotted group dir (com.amplifyframework/), mavenLocal uses a slashed
  # one (com/amplifyframework/).
  grp_slash="${group//./\/}"
  if ! grep -Eq "($group|$grp_slash)/[^/]+/$version/" "$CONSUMER/build/compile-classpath.txt"; then
    echo "SKIP [$coord]: target artifact did not resolve onto the consumer classpath (not published at $version?) — skipping to avoid a false scope failure"
    SKIP=$((SKIP+1)); continue
  fi

  if python3 "$PYCHECK" --api-file "$api" \
       --classpath "$CONSUMER/build/compile-classpath.txt" --module "$coord"; then
    PASS=$((PASS+1))
  else
    FAIL=$((FAIL+1)); FAILED="$FAILED $coord"
  fi
done < "$COORDS_FILE"

echo ""
echo "==== SCOPE CHECK: pass=$PASS fail=$FAIL skip=$SKIP ===="
if [ "$FAIL" -gt 0 ]; then
  echo "FAILED:$FAILED"
  exit 1
fi
