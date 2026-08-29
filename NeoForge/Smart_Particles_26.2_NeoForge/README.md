# Smart Particles for NeoForge 26.2

This is the Minecraft 26.2 NeoForge implementation of Smart Particles.

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

## Validation Status

The Minecraft 26.2 NeoForge JAR passed clean CI compilation, packaging, checksum verification, metadata inspection, class-content inspection, and reproducibility checks.

Validated JAR:

```text
smart_particles-26.2.11.jar
SHA-256: 457725fca4bf98953f8f62a1afe8120a0fc09f881ae566f15b408e5117aaaf62
```

A dedicated NeoForge in-game smoke test has not yet been recorded. Run the gameplay checklist in `docs/minecraft-26.2-port.md` before uploading this file to Modrinth or CurseForge.

No percentage performance claim is made until repeatable profiling is completed on controlled hardware and workloads.
