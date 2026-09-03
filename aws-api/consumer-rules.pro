# Keep org.json; R8 can obfuscate a dependency-bundled copy in release builds and break connection_ack parsing.
-keep class org.json.** { *; }
