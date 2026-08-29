# Smart Particles for Fabric 26.2

This is the Minecraft 26.2 Fabric test port of Smart Particles.

## Requirements

- Minecraft Java Edition 26.2
- Fabric Loader 0.19.3 or newer
- Java 25
- Mod Menu 20.0.1 is optional and adds the in-game Config button

Fabric API 0.158.0+26.2 is present on the development and CI classpath because Mod Menu's 26.2 artifact uses Fabric API interface injection. Smart Particles core does not call Fabric API and `fabric.mod.json` does not declare it as a hard dependency. Cloth Config is not used.

For the first gameplay test, install Fabric API and Mod Menu together. Also test the Smart Particles JAR without either optional mod to confirm standalone startup.

## Performance changes in 1.15.0

- Uses a lightweight camera-culling-only path while the particle count is already under the configured cap.
- Avoids allocating a camera direction object every tick.
- Reads each particle position once per scoring pass.
- Caches the FOV-derived camera threshold until FOV changes.
- Skips empty particle groups.
- Clears temporary heap and identity-set references after use so removed particles can be reclaimed sooner.
- Releases unusually large temporary buffers after the configured limit is reduced, including when no particles are active.
- Caps manual configuration values at 1,000,000 particles to prevent accidental oversized allocations.

## Minecraft 26.2 API changes

- Screen transitions now use `Minecraft.gui.setScreen`.
- The main render camera is accessed through a small Mixin accessor because the old public getter was removed.
- Particle queues are accessed through a small Mixin accessor because `ParticleGroup.getAll()` was removed.
- Particle-limit text is parsed and validated on save because `EditBox.setFilter` was removed.

## Build

```bash
./gradlew clean build
```

The playable JAR is written to `build/libs/`. Do not use the `-sources.jar` file in Minecraft.

## Configuration

With Mod Menu installed, open Mods, select Smart Particles, and choose Config. The JSON file remains at:

```text
.minecraft/config/smart_particles/config.json
```

Defaults:

- Particle limit: 5000
- Smart Camera Culling: enabled

## Test status

This branch is intended for local validation before publishing. The Gradle build confirms compile-time compatibility, but gameplay tests should still cover explosions, fireworks, rain, portals, modded particle spam, a zero-particle limit, and toggling camera culling.
