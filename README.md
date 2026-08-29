# Smart Particles (Source Repository)

This repository contains the source code for **Smart Particles**, a lightweight client-side optimization mod for Minecraft.

**Mod Summary:** Smart Particles caps the total particle count to maintain stable FPS. Instead of blocking new particles randomly, it intelligently removes particles that are less important to the player, prioritizing nearby and visible effects.

## Current Status

- Current source target: Minecraft 26.2
- Storefront release line at the time of this merge: Minecraft 26.1.x
- Fabric version: `1.15.0`
- NeoForge version: `26.2.11`
- Automated builds: `.github/workflows/build-26.2-test.yml`
- Clean Fabric and NeoForge builds: passed on Java 25
- Fabric gameplay validation: passed on August 29, 2026
- NeoForge gameplay validation: a dedicated in-game smoke test is not yet recorded, but clean compilation, packaging, checksum, metadata, and class-content validation passed

The Minecraft 26.2 source was developed and validated through pull request `#8`. No workflow in this repository publishes automatically to Modrinth or CurseForge.

See [`docs/minecraft-26.2-port.md`](./docs/minecraft-26.2-port.md) for the compatibility matrix, implementation notes, performance changes, exact artifact checksums, build history, gameplay validation record, and remaining publication work.

---

## Repository Structure

The codebase is organized by mod loader and Minecraft version. Each version folder is a standalone Gradle project.

### Fabric

Contains source code for the **Fabric** mod loader.

- **Supported Versions:** 1.16.5 through 1.21.x, Minecraft 26.1.x, and Minecraft 26.2.
- **Minecraft 26.2 Dependencies:** Fabric Loader, with Mod Menu optional for the configuration button.
- **Minecraft 26.2 Java:** JDK 25.
- **26.2 Project:** [`Fabric/Smart_Particles_26.2_Fabric`](./Fabric/Smart_Particles_26.2_Fabric)

The 26.2 Fabric project does not declare Fabric API or Cloth Config as hard Smart Particles runtime requirements. Fabric API is used on the development classpath because the optional Mod Menu 26.2 artifact relies on Fabric API interface injection. The Smart Particles configuration screen uses Minecraft's native GUI classes.

### Forge

Contains source code for the **Minecraft Forge** mod loader.

- **Supported Versions:** 1.7.10, 1.12.2, and releases through 1.21.x.
- Forge support is discontinued for newer Minecraft releases in favor of NeoForge.

### NeoForge

Contains source code for the **NeoForge** mod loader.

- **Supported Versions:** 1.20.4 and newer, including Minecraft 26.1.x and Minecraft 26.2.
- **Dependencies:** NeoForge only.
- **Minecraft 26.2 Java:** JDK 25.
- **26.2 Project:** [`NeoForge/Smart_Particles_26.2_NeoForge`](./NeoForge/Smart_Particles_26.2_NeoForge)
- **Configuration:** Native integration with the NeoForge Mods screen.

### WIP and Experimental

[`Smart_Particles_WIP`](./Smart_Particles_WIP) contains experimental projects that are not release-ready.

---

## How to Build

Each version is an isolated project and must be built from its own folder.

1. Navigate to the desired project folder.

   ```text
   cd Fabric/Smart_Particles_26.2_Fabric
   ```

2. Run the build.

   Windows:

   ```text
   .\gradlew.bat clean build
   ```

   Linux or macOS:

   ```text
   ./gradlew clean build
   ```

3. Find the playable JAR in that project's `build/libs/` folder.

Do not install a file whose name ends in `-sources.jar`.

---

## Validation Policy

A successful Gradle build verifies that the source compiles against the selected loader and Minecraft APIs. It does not replace gameplay testing.

For Minecraft 26.2, both loader targets passed clean CI compilation, playable-JAR discovery, checksum generation, reproducibility checks, and archive inspection. The Fabric JAR also passed maintainer gameplay validation, including configuration persistence, zero and low particle limits, major particle-effect scenarios, renderer compatibility smoke testing, and `latest.log` review. A dedicated NeoForge gameplay result should still be recorded before publishing that file to a storefront.

---

## License

### MIT License

You are free to:

- Use this mod in any modpack.
- View, fork, and modify the source code.
- Distribute built versions while keeping the license intact.
