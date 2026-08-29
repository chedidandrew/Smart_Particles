package sp;

import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SPMod implements ClientModInitializer {
    public static final String MOD_ID = "smart_particles";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeClient() {
        SPConfig.load();
        LOGGER.info("Smart Particles initialized for Minecraft 26.2");
    }
}
