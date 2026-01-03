# Particle Cap

* **Summary:** Caps the total particle count to 5000, keeping the nearest particles to the player.

**Particle Cap** is a lightweight client side optimization mod for Minecraft **(Fabric)** that keeps FPS stable in particle heavy areas. Instead of blocking new particles, it keeps the most important particles visible by removing the farthest ones first.

---

## Features

* **Smart culling:** When the limit is reached, the mod removes particles **farthest from the player** to make room for new nearby particles. Optional: Only keep particles in front of the camera and within the view frustum for improved performance.
* **Hard cap protection:** Helps prevent freezes from massive particle spam (farms, explosions, modded effects).
* **Configurable limit:** Change the maximum particle count anytime.
* **Mod Menu integration:** In game settings screen support.

---

## Configuration

* **Default limit:** `5000`
* **Minimum limit:** `0`
* **Strict Camera Culling:** `true` (Default) - Aggressively removes particles outside the camera view.

### Edit methods

1. **In game:** Install **Mod Menu**, then click the settings button for Particle Cap.
2. **Manual:** Edit `.minecraft/config/particlecap/config.json`

---

## Installation and dependencies

Requires **Fabric Loader**.

| Dependency            | Required | Purpose                                         |
| --------------------- | -------- | ----------------------------------------------- |
| **Fabric API**        | Yes      | Core Fabric utilities used by the mod           |
| **Cloth Config API**  | Optional | Settings screen and config support              |
| **Mod Menu**          | Optional | Adds the in game Mods button and settings entry |

---

## Compatibility

* **Client side only:** Not needed on servers. Works on vanilla and modded servers.
* **Shader friendly:** Compatible with Sodium and Iris.

---

## How it works

Particle Cap uses Mixins to hook into Minecraft’s `ParticleManager`. Each tick:

1. If **Strict Camera Culling** is enabled, particles outside the view frustum are removed immediately.
2. If the total particle count still exceeds the limit:
    * Measure distance from the player to each active particle
    * Identify the farthest particles
    * Remove the excess so the total stays under the cap

---

## License

MIT License. Free to use in modpacks and to view or modify the source.