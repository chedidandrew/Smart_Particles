package sp;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class SPConfig {
    public static final int DEFAULT_PARTICLE_LIMIT = 5000;
    public static final int MAX_PARTICLE_LIMIT = 1_000_000;

    private static final Path CONFIG_PATH = FMLPaths.CONFIGDIR.get()
            .resolve("smart_particles/config.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static SPConfig instance = new SPConfig();

    public int particleLimit = DEFAULT_PARTICLE_LIMIT;
    public boolean smartCameraCulling = true;

    public static SPConfig get() {
        return instance;
    }

    public static void load() {
        SPConfig loaded = new SPConfig();
        boolean shouldSave = !Files.exists(CONFIG_PATH);

        if (Files.exists(CONFIG_PATH)) {
            try {
                JsonObject json = JsonParser.parseString(Files.readString(CONFIG_PATH, StandardCharsets.UTF_8))
                        .getAsJsonObject();

                if (json.has("particleLimit")) {
                    loaded.particleLimit = json.get("particleLimit").getAsInt();
                }
                if (json.has("smartCameraCulling")) {
                    loaded.smartCameraCulling = json.get("smartCameraCulling").getAsBoolean();
                }
            } catch (IOException | RuntimeException exception) {
                SPMod.LOGGER.warn("Could not read Smart Particles config. Restoring defaults.", exception);
                shouldSave = true;
            }
        }

        instance = loaded;
        int normalizedLimit = clampParticleLimit(instance.particleLimit);
        if (normalizedLimit != instance.particleLimit) {
            instance.particleLimit = normalizedLimit;
            shouldSave = true;
        }

        if (shouldSave) {
            save();
        }
    }

    public static void save() {
        instance.particleLimit = clampParticleLimit(instance.particleLimit);

        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            Files.writeString(CONFIG_PATH, GSON.toJson(instance), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            SPMod.LOGGER.error("Could not save Smart Particles config.", exception);
        }
    }

    public static int clampParticleLimit(int value) {
        return Math.max(0, Math.min(MAX_PARTICLE_LIMIT, value));
    }
}
