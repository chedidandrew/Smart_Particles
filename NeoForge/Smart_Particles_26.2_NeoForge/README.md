# Smart Particles for NeoForge 26.2

This is the Minecraft 26.2 NeoForge test port of Smart Particles.

## Requirements

- Minecraft Java Edition 26.2
- NeoForge 26.2.0.67 or newer within the 26.2 line
- Java 25

## Performance changes in 26.2.11

- Uses a lightweight camera-culling-only path while the particle count is already under the configured cap.
- Avoids allocating a camera direction object every tick.
- Reads each particle position once per scoring pass.
- Caches the FOV-derived camera threshold until FOV changes.
- Skips empty particle groups.
- Clears temporary heap and identity-set references after use so removed particles can be reclaimed sooner.
- Releases unusually large temporary buffers after the configured limit is reduced, including when no particles are active.
- Caps manual configuration values at 1,000,000 particles to prevent accidental oversized allocations.
- Removes the unused particle-renderer accessor from the new port.

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

Open Mods, select Smart Particles, and choose Config. The JSON file remains at:

```text
.minecraft/config/smart_particles/config.json
```

Defaults:

- Particle limit: 5000
- Smart Camera Culling: enabled

## Test status

This branch is intended for local validation before publishing. The Gradle build confirms compile-time compatibility, but gameplay tests should still cover explosions, fireworks, rain, portals, modded particle spam, a zero-particle limit, and toggling camera culling.
