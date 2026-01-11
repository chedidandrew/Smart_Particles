package sp;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraftforge.fml.loading.FMLPaths; 

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class SPConfig {
    private static final Path CONFIG_PATH = FMLPaths.CONFIGDIR.get().resolve("smart_particles/config.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static SPConfig instance = new SPConfig();

    public int particleLimit = 5000;
    public boolean smartCameraCulling = true;

    public static void load() {
        if (Files.exists(CONFIG_PATH)) {
            try {
                String json = Files.readString(CONFIG_PATH);
                instance = GSON.fromJson(json, SPConfig.class);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            save();
        }

        if (instance.particleLimit < 0) {
            instance.particleLimit = 0;
            save();
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