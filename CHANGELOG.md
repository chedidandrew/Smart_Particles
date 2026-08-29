# Changelog

All notable source, build, compatibility, and performance changes are recorded here.

## Unreleased

### Minecraft 26.2 test port, 2026-08-28

#### Added

- Added the standalone Fabric project `Fabric/Smart_Particles_26.2_Fabric`.
- Added the standalone NeoForge project `NeoForge/Smart_Particles_26.2_NeoForge`.
- Added a GitHub Actions matrix build for both Minecraft 26.2 loader targets.
- Added `docs/minecraft-26.2-port.md` with the compatibility matrix, technical notes, risk assessment, manual test plan, and publication checklist.
- Added explicit configuration bounds and recovery from malformed JSON files.

#### Changed

- Targeted Minecraft Java Edition 26.2 and Java 25.
- Updated Fabric to Fabric Loader 0.19.3, Fabric Loom 1.17-SNAPSHOT, and optional Mod Menu 20.0.1 integration.
- Updated NeoForge to NeoForge 26.2.0.67 and ModDevGradle 2.0.144.
- Replaced the Fabric Cloth Config screen with a small native Minecraft screen.
- Removed Fabric API and Cloth Config as Smart Particles runtime requirements in the 26.2 Fabric project.
- Marked the NeoForge entrypoint as client-only.
- Removed the unused NeoForge particle-renderer accessor from the 26.2 project.
- Removed machine-specific Java paths from the new Gradle projects.

#### Performance

- Added an O(n) under-limit culling path instead of constructing the bounded selection heap during ordinary under-cap frames.
- Reused the camera-forward vector rather than allocating a new vector every particle tick.
- Cached the FOV-derived cone threshold until the FOV setting changes.
- Read particle coordinates once per scoring pass.
- Skipped empty particle groups before iterator creation.
- Cleared temporary heap and identity-set references after selection so discarded particles can be reclaimed sooner.
- Released unusually large temporary buffers after the configured cap is reduced.
- Limited the configurable particle cap to 1,000,000 to prevent accidental oversized allocations.

#### Preserved

- Kept every previously published source folder unchanged.
- Kept the existing JSON path: `.minecraft/config/smart_particles/config.json`.
- Kept the default 5,000-particle limit and smart camera culling enabled.
- Did not add automatic Modrinth or CurseForge publication.

#### Validation

- Source review: complete.
- Automated Fabric build: pending first branch workflow run.
- Automated NeoForge build: pending first branch workflow run.
- In-game validation: required before publication.
