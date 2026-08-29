# Minecraft 26.2 Port Record

## Purpose

This document preserves the full context for the Smart Particles Minecraft 26.2 update. It is intended to make the port reproducible and understandable months or years later.

The work is isolated on `update/minecraft-26.2-test`. Previously published source folders remain unchanged, and no storefront publication is automated.

## Target Matrix

| Target | Minecraft | Loader or toolchain | Java | Test mod version |
| --- | --- | --- | --- | --- |
| Fabric | 26.2 | Fabric Loader 0.19.3, Fabric Loom 1.17-SNAPSHOT | 25 | 1.15.0 |
| NeoForge | 26.2 | NeoForge 26.2.0.67, ModDevGradle 2.0.144 | 25 | 26.2.11 |

Fabric development integration:

- Fabric API 0.158.0+26.2 is on the build and CI classpath because Mod Menu 20.0.1 injects Fabric API interfaces into Minecraft classes.
- Mod Menu 20.0.1 provides the Mods screen configuration button.
- Smart Particles does not call Fabric API and does not declare it as a hard dependency in `fabric.mod.json`.
- Cloth Config is not used by Smart Particles 1.15.0.

## New Project Locations

```text
Fabric/Smart_Particles_26.2_Fabric
NeoForge/Smart_Particles_26.2_NeoForge
```

Each folder is an independent Gradle project with its own wrapper, metadata, source, resources, and README.

## Compatibility Work

### Shared code

- Ported the particle engine mixin to Minecraft 26.2 types, including `ParticleGroup`, `ParticleRenderType`, and tracked `ParticleLimit` counts.
- Added a `GameRenderer` Mixin accessor because Minecraft 26.2 removed the public main-camera getter while retaining a private `mainCamera` field.
- Added a `ParticleGroup` Mixin accessor because Minecraft 26.2 removed `getAll()` and keeps the particle queue as a protected field.
- Updated screen navigation to `Minecraft.gui.setScreen`.
- Corrected the utility import to `net.minecraft.util.Util`.
- Removed the obsolete `EditBox.setFilter` call and validate the particle limit when the screen saves.
- Retained explicit decrementing of tracked particle-group counts when Smart Particles removes a particle.
- Retained the existing configuration location and defaults.
- Updated the configuration parser to recover from malformed JSON instead of preventing client initialization.
- Clamped configuration values loaded from disk as well as values saved through the GUI.

### Fabric

- Updated Minecraft, Loader, Loom, Gradle, and Java metadata for 26.2.
- Kept Mod Menu as an optional integration.
- Replaced Cloth Config with a native screen using Minecraft GUI controls.
- Removed Fabric API as a hard Smart Particles metadata dependency because the core implementation does not call Fabric API classes.
- Added Fabric API to the development classpath to satisfy Mod Menu 20.0.1's interface-injection compile requirements.
- Updated `fabric.mod.json` to declare Minecraft 26.2 and Java 25.

### NeoForge

- Updated Minecraft, NeoForge, ModDevGradle, Gradle, and Java metadata for 26.2.
- Declared the mod entrypoint with `dist = Dist.CLIENT`.
- Registered the configuration screen through `IConfigScreenFactory`.
- Limited NeoForge compatibility to the 26.2 loader line.
- Removed the unused `ParticleRendererAccessor` mixin from the new project.

## Performance Review

### Previous hot-path costs

The prior implementation could perform the full bounded nearest-particle selection path during many ordinary frames. That path requires heap maintenance and may require a second removal pass. It also repeatedly created or retained temporary objects and recalculated camera data that rarely changes.

### Implemented improvements

1. **Under-limit fast path**

   When culling is disabled and the current count is at or below the cap, the mixin exits after counting. When smart culling is enabled but the count is below the cap, it performs only the linear camera-culling pass. It does not construct the selection heap.

2. **Lower temporary allocation rate**

   The camera-forward `Vector3f` is retained and reused. Particle x, y, and z values are read once per evaluation instead of repeatedly calling mixin accessors.

3. **Cached camera threshold**

   The cosine threshold derived from FOV is recalculated only when the configured FOV changes.

4. **Cleaner traversal**

   Empty particle groups are skipped before creating iterators. Player and camera coordinates are copied to local primitives before the loops.

5. **Reduced object retention**

   Heap slots are cleared after selection. The identity set is cleared after use. Oversized arrays and sets are released when the user substantially lowers the particle cap, including when no particles are active.

6. **Defensive configuration ceiling**

   The cap is restricted to 1,000,000 particles. This still permits extreme stress testing while avoiding accidental array allocations caused by malformed or manually edited values.

### Complexity

- Counting remains O(n), where n is the number of active particles.
- Under-limit camera culling is O(n).
- Over-limit nearest-particle selection remains O(n log k), where k is the configured particle cap.
- Memory for over-limit selection remains O(k).

### Changes deliberately not made

- No background threads touch Minecraft particle collections. Those collections are owned by the client thread, and parallel mutation would introduce correctness and compatibility risk.
- No fixed multi-tick delay was added to enforcement. A delay could reduce CPU use, but it would also allow larger transient particle spikes and should be evaluated with profiling data first.
- No spatial partitioning cache was added. Maintaining such a cache may cost more than it saves for normal particle counts and would increase mixin complexity.

## Clean-Build History

1. The first Fabric build failed during Gradle configuration because Loom 1.17 removed `modImplementation`.
2. Commit `2d57fcf` changed the optional Mod Menu dependency to the current `implementation` configuration and updated the GitHub Actions majors.
3. The second Fabric build reached Java compilation. It exposed the Mod Menu Fabric API interface-injection requirement and the shared Minecraft 26.2 API removals.
4. The first two NeoForge builds reached Java compilation and exposed the same shared Minecraft API removals.
5. The next matrix run validates the new main-camera accessor, particle-queue accessor, GUI navigation, utility import, save-time input validation, and Fabric API development classpath.

## Automated Build

Workflow:

```text
.github/workflows/build-26.2-test.yml
```

The workflow builds Fabric and NeoForge independently on Java 25 and uploads only playable JARs, excluding source JARs. Artifacts are retained for 14 days.

Local commands:

```bash
cd Fabric/Smart_Particles_26.2_Fabric
./gradlew clean build

cd ../../NeoForge/Smart_Particles_26.2_NeoForge
./gradlew clean build
```

## Required Gameplay Tests

Run these checks separately with the Fabric and NeoForge JARs:

- Start Minecraft 26.2 and reach the title screen without mixin or metadata errors.
- Open the Mods screen and confirm Smart Particles metadata and icon are present.
- Open the configuration screen, save both settings, restart, and confirm persistence.
- On Fabric, test with Fabric API and Mod Menu installed.
- On Fabric, test Smart Particles without Fabric API and Mod Menu to verify standalone startup.
- Test the default 5,000-particle cap.
- Test a small cap such as 100.
- Test a zero cap.
- Disable smart camera culling and verify particles remain until the global cap is exceeded.
- Re-enable smart camera culling and rotate the camera around active particle effects.
- Test explosions, fireworks, campfire smoke, portals, rain, thunderstorms, underwater particles, and block breaking.
- Test a heavy particle source or a particle-spam command from another test mod.
- Confirm nearby rain and weather effects are not removed too aggressively.
- Change the cap from a very high value to a low value and watch memory behavior.
- Leave a world, join another world, and verify no stale particle state or crash.
- Test with Sodium or another common renderer on Fabric.
- Test with Embeddium or another common renderer on NeoForge when a 26.2-compatible release is available.

## Profiling Plan

For measurable performance claims, compare the previous and 26.2 builds under the same particle workload and settings. Record:

- Average client tick time.
- 95th and 99th percentile frame time.
- Allocation rate.
- Garbage collection pauses.
- Active particle count.
- CPU sample time attributed to `ParticleEngine.tick` and `ParticleManagerMixin`.

Do not publish a percentage improvement without repeatable measurements from the same hardware, world, camera path, mod list, and particle workload.

## Publication Checklist

- Both GitHub Actions jobs pass from a clean checkout.
- Both downloaded JARs pass the gameplay checklist.
- No critical errors appear in `latest.log`.
- Confirm the final version numbers and filenames.
- Update this document and `CHANGELOG.md` with the gameplay results.
- Merge the test pull request only after validation.
- Upload the exact tested JARs to Modrinth and CurseForge.
- Mark the release as Minecraft 26.2 and select the correct loader for each file.
- Preserve the SHA-256 checksums of the published files.
