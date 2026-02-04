# Smart Particles (1.20.1)

* **Summary:** A performance mod that caps the total particle count, keeping the nearest particles in the player's view visible.

**Smart Particles** is a lightweight client-side optimization mod for Minecraft **1.20.1 (Forge)** that keeps FPS stable in particle-heavy areas. Instead of blocking new particles, it keeps the most important particles visible by removing the farthest ones first.

**Standalone Version:** No extra dependencies required. Just drop it in the `mods` folder!

---

## Features

* **Smart culling:** When the limit is reached, the mod removes particles **farthest from the player** to make room for new nearby particles.
* **Frustum Culling:** Optional setting to aggressively remove particles that are outside the camera's view (behind you).
* **Hard cap protection:** Helps prevent freezes from massive particle spam (farms, explosions, modded effects).
* **Configurable limit:** Change the maximum particle count anytime.
* **In-Game Config:** Native Forge "Mod Options" support.

---

## Configuration

* **Default limit:** `5000`
* **Minimum limit:** `0`
* **Smart Camera Culling:** `true` (Default) - Aggressively removes particles outside the camera view.
    * *Note: This is labeled as "Smart Culling" in the in-game config screen.*

### Edit methods

1. **In-game:** Click the **Mods** button in the main menu, select **Smart Particles**, and click **Config**.
2. **Manual:** Edit `.minecraft/config/smart_particles/config.json`.

---

## Compatibility

* **Client-side only:** Not needed on servers. Works on vanilla and modded servers.
* **Shader friendly:** Compatible with OptiFine and standard shaders.

---

## How it works

Smart Particles accesses Minecraft’s internal particle list directly. Each tick:

1. If **Smart Camera Culling** is enabled, particles outside the view frustum are removed immediately.
2. If the total particle count still exceeds the limit:
    * Measure distance from the player to each active particle
    * Identify the farthest particles
    * Remove the excess so the total stays under the cap

---

## License

MIT License. Free to use in modpacks and to view or modify the source.