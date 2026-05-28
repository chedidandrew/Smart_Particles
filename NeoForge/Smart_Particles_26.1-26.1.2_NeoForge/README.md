# Smart Particles

* **Summary:** A performance mod that caps the total particle count, keeping the nearest particles in the player's view visible.

**Smart Particles** is a lightweight client side optimization mod for Minecraft **(NeoForge)** that keeps FPS stable in particle heavy areas. Instead of blocking new particles, it keeps the most important particles visible by removing the farthest ones first.

---

## Features

* **Smart culling:** When the limit is reached, the mod removes particles **farthest from the player** to make room for new nearby particles. Optional: Only keep particles in front of the camera and within the view frustum for improved performance.
* **Hard cap protection:** Helps prevent freezes from massive particle spam (farms, explosions, modded effects).
* **Configurable limit:** Change the maximum particle count anytime.
* **Native configuration:** In-game settings screen support via the NeoForge mod list.

---

## Configuration

* **Default limit:** `5000`
* **Minimum limit:** `0`
* **Smart Camera Culling:** `true` (Default) - Aggressively removes particles outside the camera view.

### Edit methods

1. **In game:** Go to the main menu, click **Mods**, select **Smart Particles**, and click **Config**.
2. **Manual:** Edit `.minecraft/config/smart_particles/config.json`

---

## Installation and dependencies

Requires **NeoForge**.

| Dependency            | Required | Purpose                                         |
| --------------------- | -------- | ----------------------------------------------- |
| **NeoForge**          | Yes      | The mod loader                                  |
| **Java 25**           | Yes      | Required for Minecraft 26.x                     |

*No other dependencies required (Cloth Config is not needed).*

### Supported versions

This shared NeoForge project supports Minecraft / NeoForge calendar versions **26.1**, **26.1.1**, and **26.1.2** from one codebase. It builds by default against the latest available 26.1.2 target and emits compatibility jars for all three supported filenames:

* `smart_particles-26.1.10.jar`
* `smart_particles-26.1.1.10.jar`
* `smart_particles-26.1.2.10.jar`

### Build

On Windows:

```powershell
.\gradlew.bat clean build
```

The compiled jars are written to `build/libs/`.

---

## Compatibility

* **Client side only:** Not needed on servers. Works on vanilla and modded servers.
* **Shader friendly:** Compatible with Embeddium and Oculus.

---

## How it works

Smart Particles uses Mixins to hook into Minecraft’s `ParticleManager`. Each tick:

1. If **Smart Camera Culling** is enabled, particles outside the view frustum are removed, **unless they are very close (within ~4 blocks) to the player**.
2. If the total particle count still exceeds the limit:
    * Measure distance from the player to each active particle
    * Identify the farthest particles
    * Remove the excess so the total stays under the cap

---

## License

MIT License. Free to use in modpacks and to view or modify the source.
