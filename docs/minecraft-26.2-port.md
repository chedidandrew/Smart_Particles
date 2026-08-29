# Minecraft 26.2 Port Record

## Purpose and Integration Status

This document preserves the full context for the Smart Particles Minecraft 26.2 update so the work remains reproducible and understandable months or years later.

The port was developed on `update/minecraft-26.2-test` and integrated through pull request `#8`. Previously published source folders were left unchanged. No repository workflow publishes automatically to Modrinth or CurseForge.

## Target Matrix

| Target | Minecraft | Loader or toolchain | Java | Mod version |
| --- | --- | --- | --- | --- |
| Fabric | 26.2 | Fabric Loader 0.19.3, Fabric Loom 1.17-SNAPSHOT | 25 | 1.15.0 |
| NeoForge | 26.2 | NeoForge 26.2.0.67, ModDevGradle 2.0.144 | 25 | 26.2.11 |

Fabric development integration:

- Fabric API 0.158.0+26.2 is on the build and CI classpath because Mod Menu 20.0.1 injects Fabric API interfaces into Minecraft classes.
- Mod Menu 20.0.1 provides the optional Mods screen configuration button.
- Smart Particles does not call Fabric API and does not declare it as a hard dependency in `fabric.mod.json`.
- Cloth Config is not used by Smart Particles 1.15.0.

## Project Locations

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

   Empty particle groups are skipped before creating iterators. Player and camera coordinates are copied to local primitive values before the loops.

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

## Build and Validation History

1. The first Fabric build failed during Gradle configuration because Loom 1.17 removed `modImplementation`.
2. Commit `2d57fcf` changed the optional Mod Menu dependency to the current `implementation` configuration and updated the GitHub Actions majors.
3. The second Fabric build reached Java compilation. It exposed the Mod Menu Fabric API interface-injection requirement and the shared Minecraft 26.2 API removals.
4. The first two NeoForge builds reached Java compilation and exposed the same shared Minecraft API removals.
5. Commit `e325640a606ebbbe848ea933983f3fbccfad3e7e` added the main-camera accessor, particle-queue accessor, GUI navigation update, utility import correction, save-time input validation, and Fabric API development classpath.
6. GitHub Actions push run `33235002059` passed both loader jobs from a clean checkout on Java 25.
7. Fabric reported `BUILD SUCCESSFUL` after compile, resource processing, JAR generation, and checks.
8. NeoForge reported `BUILD SUCCESSFUL` after Minecraft artifact creation, compile, resource processing, JAR generation, and checks.
9. Both jobs found a playable JAR, generated a SHA-256 file, and uploaded the artifact.
10. Downloaded artifacts were independently inspected. Their JAR hashes matched the workflow checksum files, required metadata was present, and expected Smart Particles classes and mixins were included.
11. Documentation commit `47d4505d62635277b0bef0af3efc55349c55f00a` passed both loader jobs in GitHub Actions run `33235166995`.
12. The runtime JARs rebuilt from that documentation commit were byte-for-byte identical to the first successful artifacts.
13. On August 29, 2026, the maintainer confirmed the Fabric JAR works in Minecraft 26.2.
14. Pull request `#8` records successful Fabric checks for configuration persistence, zero and low particle limits, explosions, fireworks, portals, rain, thunderstorms, block breaking, renderer compatibility smoke testing, and `latest.log` review.
15. A dedicated NeoForge gameplay result has not been recorded. NeoForge clean compilation and binary validation remain successful.

## Validated Artifacts

These binaries were generated from code commit `e325640a606ebbbe848ea933983f3fbccfad3e7e` in GitHub Actions push run `33235002059`.

| Loader | Runtime JAR | Size | SHA-256 | Workflow artifact ID |
| --- | --- | ---: | --- | ---: |
| Fabric | `smart_particles+mc26.2-1.15.0.jar` | 56,094 bytes | `58a22aeba0ccc6d3c19edc4849163507ccc6081f3ebbb0c8424e3b8e9a6ce7f4` | `9709646600` |
| NeoForge | `smart_particles-26.2.11.jar` | 55,384 bytes | `457725fca4bf98953f8f62a1afe8120a0fc09f881ae566f15b408e5117aaaf62` | `9709648532` |

Workflow archive digests:

- Fabric ZIP artifact: `sha256:1263edc0582682f6869df4495bac7ee7cc5616e519fceda8ccb4e37873c17596`.
- NeoForge ZIP artifact: `sha256:095b6e31a71f50211721d51788f60a3e4983ea09c36fbc89b263b09cd0b81bbb`.

Validation completed for both artifacts:

- Clean compilation.
- Playable-JAR discovery.
- Workflow and independently recalculated SHA-256 agreement.
- Metadata inspection.
- Expected entrypoint, configuration, accessor, and particle mixin class inspection.
- Reproducible runtime JAR output across two successful CI runs.

Additional Fabric validation:

- In-game launch and gameplay operation confirmed by the maintainer.
- Configuration persistence confirmed.
- Zero and low particle limits confirmed.
- Major vanilla particle scenarios confirmed.
- Renderer compatibility smoke test confirmed.
- `latest.log` review confirmed.

NeoForge status:

- Build and package validation passed.
- A dedicated in-game smoke test is still recommended before uploading the NeoForge JAR to Modrinth or CurseForge.

## Automated Build

Workflow:

```text
.github/workflows/build-26.2-test.yml
```

The workflow builds Fabric and NeoForge independently on Java 25 and uploads only playable JARs, excluding source JARs. It runs for relevant changes on `main`, the original port branch, pull requests, and manual dispatches.

Validated runs:

```text
GitHub Actions run: 33235002059
Code commit: e325640a606ebbbe848ea933983f3fbccfad3e7e
Result: Fabric success, NeoForge success

GitHub Actions run: 33235166995
Documentation commit: 47d4505d62635277b0bef0af3efc55349c55f00a
Result: Fabric success, NeoForge success, byte-for-byte identical runtime JARs
```

Local commands:

```bash
cd Fabric/Smart_Particles_26.2_Fabric
./gradlew clean build

cd ../../NeoForge/Smart_Particles_26.2_NeoForge
./gradlew clean build
```

## Remaining Gameplay and Regression Tests

The Fabric smoke test is complete. These checks should still be repeated after future particle-engine or Minecraft mapping changes:

- Start Minecraft and reach the title screen without mixin or metadata errors.
- Open the Mods screen and confirm Smart Particles metadata and icon.
- Open the configuration screen, save both settings, restart, and confirm persistence.
- Test the default 5,000-particle cap.
- Test a small cap such as 100.
- Test a zero cap.
- Test smart camera culling both enabled and disabled.
- Test explosions, fireworks, campfire smoke, portals, rain, thunderstorms, underwater particles, and block breaking.
- Test a heavy particle source or particle-spam test mod.
- Confirm nearby rain and weather effects are not removed too aggressively.
- Change the cap from a very high value to a low value and observe memory behavior.
- Leave a world, join another world, and verify no stale particle state or crash.
- Review `latest.log`.

Before publishing the NeoForge file, run the same list with NeoForge 26.2 and include a common 26.2-compatible renderer when one is available.

## Profiling Plan

For measurable performance claims, compare the previous and 26.2 builds under the same particle workload and settings. Record:

- Average client tick time.
- 95th and 99th percentile frame time.
- Allocation rate.
- Garbage collection pauses.
- Active particle count.
- CPU sample time attributed to `ParticleEngine.tick` and `ParticleManagerMixin`.

Do not publish a percentage improvement without repeatable measurements from the same hardware, world, camera path, mod list, and particle workload.

## Integration and Publication Checklist

- [x] Both GitHub Actions jobs pass from a clean checkout.
- [x] Exact JAR checksums are recorded.
- [x] Downloaded JAR metadata and class contents are inspected.
- [x] Runtime JAR reproducibility is verified.
- [x] Fabric gameplay validation is recorded.
- [x] Fabric configuration persistence, limit behavior, particle effects, renderer smoke test, and `latest.log` review are recorded.
- [x] Version numbers and filenames are recorded.
- [x] Repository README, loader READMEs, changelog, workflow, and this technical record are updated.
- [x] Source integration is approved through pull request `#8`.
- [ ] NeoForge gameplay validation is recorded.
- [ ] Upload the exact tested files to Modrinth and CurseForge when storefront publication is intended.
- [ ] Select Minecraft 26.2 and the correct loader for each storefront file.
- [ ] Preserve the published file SHA-256 checksums.
