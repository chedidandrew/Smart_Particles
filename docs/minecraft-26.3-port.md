# Minecraft 26.3 port

## Versions

| Component | Fabric | NeoForge |
| --- | --- | --- |
| Minecraft | 26.3 | 26.3 |
| Smart Particles | 1.16.0 | 26.3.0 |
| Loader | 0.19.5 | 26.3.0.4-beta |
| Build plugin | Loom 1.17.21 | ModDevGradle 2.0.147 |
| Gradle | 9.6.0 | 9.2.1 |
| Java | 25 | 25 |
| Optional configuration integration | Mod Menu 21.0.0-beta.1 | Built-in Mods screen |

Fabric API 0.160.7+26.3 is on the Fabric development classpath for Mod Menu interface injection. Smart Particles itself does not declare Fabric API or Mod Menu as a hard runtime dependency. The current NeoForge and Mod Menu dependencies are beta releases.

Dependency versions were checked against the publishers' Maven metadata on September 17, 2026. Fabric's [26.3 porting notes](https://www.fabricmc.net/2026/09/15/263.html) recommend Loom 1.17, Gradle 9.6.0, and Loader 0.19.5.

## Behavior preservation

Both projects were copied from their corresponding tracked 26.2 source trees. The old projects remain untouched. SHA-256 comparisons confirmed that SPConfig.java and all four mixin Java files are byte-for-byte identical to their respective 26.2 versions. Both loaders also retain identical particle mixin implementations.

This preserves the 5000-particle default, enabled camera culling, 0-to-1,000,000 bounds, JSON path and fields, nearest/visible particle priorities, clear-weather and storm protection distances, heap selection, particle-limit accounting, and temporary-buffer management. No new per-tick work was introduced.

The only gameplay-source adaptations are in SPConfigScreen: Minecraft 26.3 takes a URI in ConfirmLinkScreen and opens links through Blaze3D.openUri instead of Util.getPlatform().openUri. The confirmation dialog, return screen, links, save/reset/cancel behavior, and native widgets remain the same. SPMod's initialization message names 26.3. NeoForge metadata uses iconFile instead of the deprecated logoFile for the existing square icon.

ModDevGradle 2.0.144 failed while recompiling Minecraft's HolderSet anonymous subclass, before compiling this mod. Version 2.0.147 resolves that dependency compilation failure without patches to Minecraft or the particle algorithm.

## Validation performed locally

- Both standalone projects passed clean compilation and packaging using Java 25. NeoForge was rebuilt after its icon metadata adjustment.
- Inspected the playable JAR metadata, version constraints, and all four packaged mixin classes. Generated SHA256SUMS.txt beside each playable JAR.
- Inspected Minecraft 26.3 bytecode: ParticleEngine.tick, particles, trackedParticleCounts, ParticleGroup.particles, and GameRenderer.mainCamera still match the retained mixin targets.
- Started both isolated Gradle development clients. Logs confirmed Smart Particles initialization for 26.3, graphics/window initialization, resource loading, and particle-atlas creation. Both generated the expected configuration: particleLimit 5000 and smartCameraCulling true. No Smart Particles mixin failure was observed.
- Development runs reported Windows OSHI performance-counter/system-report warnings; Fabric also reported an offline-development Realms authentication warning. Neither stopped initialization. The NeoForge logoFile deprecation warning was addressed in the final artifact.
- Only the development clients started for this validation were stopped. They did not open existing user worlds. Their forced shutdown produces a nonzero runClient exit code; it is not a clean-exit gameplay test.
- Logs remain locally under each project's build/ and run/logs/ directories. They are not committed.

Interactive gameplay and the configuration screen could not be exercised: the desktop-control Node helper exited unexpectedly on both attempts. There are no automated gameplay tests in these projects. Source parity and successful startup support compatibility, but do not prove identical visual behavior or frame times across Minecraft releases. Fabric without Mod Menu/API, renderer mods, and the final NeoForge icon presentation still need interactive verification.

## Remaining gameplay checks (both loaders)

1. Start Minecraft 26.3 with the corresponding playable JAR. For Fabric, test with Mod Menu for the configuration button and separately with only Fabric Loader and Smart Particles.
2. Open the configuration screen. Check save/reopen, reset defaults, Escape without saving, invalid/empty input, and the external-link confirmation's cancel/return behavior.
3. Check limits 0, 1, 100, and 5000, with camera culling enabled and disabled. Generate sustained explosions, fireworks, portal and block-breaking particles. Confirm nearby and visible effects receive the same priority as 26.2.
4. Check clear weather, rain, and thunderstorms; turn the camera and change FOV. Check first- and third-person views and under-cap camera culling.
5. Reduce a large configured cap while effects are active, change worlds, and reload the game. Confirm configuration persists and no stale particles or crashes appear.
6. Repeat representative scenes with the user's usual renderer mods and inspect latest.log for mixin exceptions or particle errors. Compare equivalent 26.2 scenes and settings before asserting visual or measured performance parity.

## Artifacts

- Fabric: `Fabric/Smart_Particles_26.3_Fabric/build/libs/smart_particles+mc26.3-1.16.0.jar`
  - SHA-256: `418e859e4e8a59026a4643c5864245b9e577a93d4d8546026667f51e32247a88`
- NeoForge: `NeoForge/Smart_Particles_26.3_NeoForge/build/libs/smart_particles-26.3.0.jar`
  - SHA-256: `63ef86715e7e25f94c9d43c434f950f2f872dbf7f880a32242e2311169a45424`

Do not install the sources JAR. The new build-26.3-test.yml workflow builds both projects and uploads playable JARs and checksums; it does not publish releases. CI execution and storefront publication have not been performed for this port.
