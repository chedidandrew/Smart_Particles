package sp;

import net.minecraftforge.common.config.Configuration;
import java.io.File;

public class SPConfig {
    public static Configuration config;
    public static int particleLimit = 4000;
    public static boolean smartCameraCulling = true;

    public static void init(File file) {
        config = new Configuration(file);
        syncConfig();
    }

    public static void syncConfig() {
        String category = Configuration.CATEGORY_GENERAL;
        particleLimit = config.getInt("particleLimit", category, 4000, 0, Integer.MAX_VALUE, "Max particles");
        smartCameraCulling = config.getBoolean("smartCameraCulling", category, true, "Smart culling");

        if (config.hasChanged()) {
            config.save();
        }
    }
}