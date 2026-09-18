# Smart Particles for NeoForge 26.3

Requires Minecraft Java Edition 26.3 and Java 25. NeoForge 26.3.0.4-beta or newer within the 26.3 line. Configuration is available through the built-in Mods screen.

This port preserves the particle algorithm and configuration from the corresponding 26.2 project. Defaults remain 5000 particles with Smart Camera Culling enabled. Configuration remains at `.minecraft/config/smart_particles/config.json`.

## Build

Run `.\gradlew.bat clean build` on Windows or `./gradlew clean build` on Linux/macOS with JDK 25. Playable JARs are written to `build/libs/`; do not install the `-sources.jar`.

## Validation

Compilation, packaging, metadata inspection, source parity, and development-client startup passed locally. On September 18, 2026, the maintainer tested the NeoForge build in game and confirmed it passed. See [the 26.3 port record](../../docs/minecraft-26.3-port.md) for exact dependencies, API adaptations, checksums, and the maintainer gameplay validation record.
