# Smart Particles (1.7.10)

* **Summary:** A performance mod that caps the total particle count, preventing the 4,000 particle limit crash while keeping the nearest particles visible.

**Smart Particles** is a lightweight client-side optimization mod for Minecraft **1.7.10 (Forge)** that keeps FPS stable in particle-heavy areas. 

In Minecraft 1.7.10, the game engine has a **hard limit of 4,000 particles** per layer; exceeding this often causes particles to disappear randomly. This mod fixes that by intelligently removing the **farthest** particles first to make room for new ones nearby, ensuring you always see what's in front of you.

**Standalone Version:** No extra dependencies (no UniMixins or CoreMods) required. Just drop it in the `mods` folder!

---

## Features

* **Smart Culling:** When the limit is reached, the mod removes particles **farthest from the player** to make room for new nearby particles.
* **Frustum Culling:** Optional setting to aggressively remove particles that are outside the camera's view (behind you).
* **Hard Cap Protection:** Prevents the "4,000 particle limit" flickering common in 1.7.10.
* **Configurable Limit:** Change the maximum particle count anytime.
* **In-Game Config:** Native Forge "Mod Options" support (Mods -> Smart Particles -> Config).

---

## Configuration

* **Default limit:** `4000` (Matches the vanilla 1.7.10 engine limit)
* **Minimum limit:** `0`
* **Smart Camera Culling:** `true` (Default) - Aggressively removes particles outside the camera view.

### How to Edit

1. **In-game:** Click the **Mods** button in the main menu, select **Smart Particles**, and click **Config**.
2. **Manual:** Edit `.minecraft/config/smartparticles.cfg`.

---

## Compatibility

* **Client-side only:** Not needed on servers. Works on vanilla and modded servers.
* **Shader friendly:** Compatible with OptiFine and standard shaders.

---

## How it works

Smart Particles uses pure Forge events and Reflection to access Minecraft’s internal particle list. Each tick:

1. If **Smart Camera Culling** is enabled, particles outside the view frustum are removed immediately.
2. If the total particle count still exceeds the limit:
   * Measure distance from the player to each active particle
   * Identify the farthest particles
   * Remove the excess so the total stays under the cap

---

## License

MIT License. Free to use in modpacks and to view or modify the source.