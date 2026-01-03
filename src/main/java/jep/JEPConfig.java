package com.chedidandrew.particlecap;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ParticleCapConfig {
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("particlecap/config.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static ParticleCapConfig instance = new ParticleCapConfig();

    // The setting accessible to the user
    public int particleLimit = 5000;
    public boolean strictCameraCulling = true;

    public static void load() {
        if (Files.exists(CONFIG_PATH)) {
            try {
                String json = Files.readString(CONFIG_PATH);
                instance = GSON.fromJson(json, ParticleCapConfig.class);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            save();
        }

        if (instance.particleLimit < 0) {
            instance.particleLimit = 0;
            save(); // Rewrite the file with the corrected value
        }
    }

    public static void save() {
        try {
            if (!Files.exists(CONFIG_PATH.getParent())) {
                Files.createDirectories(CONFIG_PATH.getParent());
            }
            Files.writeString(CONFIG_PATH, GSON.toJson(instance));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}