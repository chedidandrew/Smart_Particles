# Smart Particles (Source Repository)

This repository contains the source code for **Smart Particles**, a lightweight client-side optimization mod for Minecraft.

**Mod Summary:** A performance mod that caps the total particle count to maintain stable FPS. Instead of blocking new particles randomly, it intelligently removes the particles farthest from the player to ensure the most important visual effects remain visible.

---

## Repository Structure

The codebase is organized by Mod Loader and then by Minecraft Version. Each sub-folder is a standalone Gradle project.

### 📂 [Fabric](./Fabric)

Contains source code for the **Fabric** mod loader.

* **Supported Versions:** 1.16.5 up to 1.21.x
* **Key Dependencies:** Fabric API, Cloth Config (optional for some versions).

### 📂 [Forge](./Forge)

Contains source code for the **Minecraft Forge** mod loader.

* **Supported Versions:** 1.7.10, 1.12.2, up to 1.21.x (Discontinued in favor of NeoForge for newer versions).

### 📂 [NeoForge](./NeoForge)

Contains source code for the **NeoForge** mod loader.

* **Supported Versions:** 1.20.4+
* **Features:** Native GUI integration via the mod list.

### 📂 [WIP / Experimental](./Smart_Particles_WIP)

Contains experimental branches or work-in-progress ports that are not yet release-ready.

---

## How to Build

Since each version is an isolated project, you must build them individually.

1. **Navigate** to the specific version folder you want to build.
   * *Example:* `cd Fabric/Smart_Particles_1.21.1_Fabric`

2. **Run the build command**:
   * **Windows:** `gradlew build`
   * **Linux/macOS:** `./gradlew build`

3. **Locate the Output:**
   * The compiled `.jar` file will be found in the `build/libs/` directory inside that specific project folder.

---

## License

### MIT License

You are free to:

* Use this mod in any modpack.
* View, fork, and modify the source code.
* Distribute built versions (keeping the license intact).
