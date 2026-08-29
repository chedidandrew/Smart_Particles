package sp;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(value = SPMod.MOD_ID, dist = Dist.CLIENT)
public final class SPMod {
    public static final String MOD_ID = "smart_particles";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public SPMod(ModContainer modContainer) {
        SPConfig.load();
        modContainer.registerExtensionPoint(IConfigScreenFactory.class,
                (minecraft, parent) -> new SPConfigScreen(parent));
        LOGGER.info("Smart Particles initialized for Minecraft 26.2");
    }
}
