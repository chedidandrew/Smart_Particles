# Changelog

All notable source, build, compatibility, validation, and performance changes are recorded here.

## Unreleased

No unreleased changes are currently recorded.

## Minecraft 26.2 GitHub source update, 2026-08-29

### Added

- Added the standalone Fabric project `Fabric/Smart_Particles_26.2_Fabric`.
- Added the standalone NeoForge project `NeoForge/Smart_Particles_26.2_NeoForge`.
- Added a GitHub Actions matrix build for both Minecraft 26.2 loader targets.
- Added `docs/minecraft-26.2-port.md` with the compatibility matrix, technical notes, risk assessment, validation history, manual test plan, exact artifact hashes, and publication checklist.
- Added explicit configuration bounds and recovery from malformed JSON files.
- Added Mixin accessors for the Minecraft 26.2 main camera and protected `ParticleGroup` queue.

### Changed

- Targeted Minecraft Java Edition 26.2 and Java 25.
- Updated Fabric to Fabric Loader 0.19.3, Fabric Loom 1.17-SNAPSHOT, Fabric API 0.158.0+26.2 on the development classpath, and optional Mod Menu 20.0.1 integration.
- Updated NeoForge to NeoForge 26.2.0.67 and ModDevGradle 2.0.144.
- Replaced the Fabric Cloth Config screen with a small native Minecraft screen.
- Removed Fabric API and Cloth Config as hard Smart Particles runtime requirements in the 26.2 Fabric metadata.
- Marked the NeoForge entrypoint as client-only.
- Removed the unused NeoForge particle-renderer accessor from the 26.2 project.
- Removed machine-specific Java paths from the new Gradle projects.
- Updated the validation workflow to `actions/checkout@v7`, `actions/setup-java@v6`, and `actions/upload-artifact@v7`.
- Updated the workflow so Minecraft 26.2 builds continue to run for relevant changes on `main`.
- Updated screen transitions for Minecraft 26.2's `Minecraft.gui.setScreen` API.
- Moved particle-limit text validation to save time after Minecraft removed `EditBox.setFilter`.

### Fixed during clean-build validation

- Replaced Fabric Loom's removed `modImplementation` configuration with the current `implementation` configuration for optional Mod Menu development integration.
- Added Fabric API to the Fabric development classpath because Mod Menu 20.0.1 injects Fabric API interfaces into Minecraft classes during compilation.
- Replaced the removed `GameRenderer.getMainCamera()` call with a zero-reflection Mixin accessor.
- Replaced the removed `ParticleGroup.getAll()` calls with a zero-reflection Mixin accessor to the protected queue.
- Corrected the Minecraft 26.2 `Util` package import.

### Performance

- Added an O(n) under-limit culling path instead of constructing the bounded selection heap during ordinary under-cap frames.
- Reused the camera-forward vector rather than allocating a new vector every particle tick.
- Cached the FOV-derived cone threshold until the FOV setting changes.
- Read particle coordinates once per scoring pass.
- Skipped empty particle groups before iterator creation.
- Cleared temporary heap and identity-set references after selection so discarded particles can be reclaimed sooner.
- Released unusually large temporary buffers after the configured cap is reduced, including frames with no active particles.
- Limited the configurable particle cap to 1,000,000 to prevent accidental oversized allocations.

### Preserved

- Kept every previously published source folder unchanged.
- Kept the existing JSON path: `.minecraft/config/smart_particles/config.json`.
- Kept the default 5,000-particle limit and smart camera culling enabled.
- Did not add automatic Modrinth or CurseForge publication.

### Validation history

- Initial Fabric clean build failed because Loom 1.17 removed `modImplementation`; corrected in commit `2d57fcf`.
- Second Fabric clean build reached Java compilation and exposed Minecraft 26.2 API changes plus Mod Menu's Fabric API compile requirement.
- Initial and second NeoForge clean builds reached Java compilation and exposed the same Minecraft 26.2 API changes.
- Code commit `e325640a606ebbbe848ea933983f3fbccfad3e7e` passed the complete Fabric and NeoForge matrix in GitHub Actions push run `33235002059`.
- Documentation commit `47d4505d62635277b0bef0af3efc55349c55f00a` also passed both loader jobs in GitHub Actions run `33235166995`.
- The second successful build produced runtime JARs that were byte-for-byte identical to the first successful build.
- Fabric completed `clean build`, playable-JAR verification, SHA-256 generation, and artifact upload successfully.
- NeoForge completed `clean build`, playable-JAR verification, SHA-256 generation, and artifact upload successfully.
- The downloaded JAR checksums matched the workflow checksum files, and both archives passed metadata and class-content inspection.
- Fabric JAR: `smart_particles+mc26.2-1.15.0.jar`, SHA-256 `58a22aeba0ccc6d3c19edc4849163507ccc6081f3ebbb0c8424e3b8e9a6ce7f4`.
- NeoForge JAR: `smart_particles-26.2.11.jar`, SHA-256 `457725fca4bf98953f8f62a1afe8120a0fc09f881ae566f15b408e5117aaaf62`.
- On August 29, 2026, the maintainer confirmed the Fabric build works in Minecraft 26.2.
- The Fabric validation record includes configuration persistence, zero and low particle limits, explosions, fireworks, portals, rain, thunderstorms, block breaking, renderer compatibility smoke testing, and review of `latest.log`.
- A dedicated NeoForge gameplay test is not yet recorded. Its clean build and binary validation remain successful.
