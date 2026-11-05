# Third-party components

The Gradle 8.10 wrapper scripts and wrapper JAR originate from the official
RuneLite example-plugin template and Gradle project. Gradle is licensed under
Apache License 2.0; see `third-party/gradle-LICENSE` and `gradle-NOTICE`.
The wrapper distribution checksum is pinned in its properties file.

RuneLite and its transitive dependencies are resolved at build time from
RuneLite's official Maven repository and Maven Central. They are not bundled
in the plugin JAR. The JAR is a RuneLite plugin, not a standalone launcher.
JDKs, Python and Gradle distributions are not included in the ZIP.
