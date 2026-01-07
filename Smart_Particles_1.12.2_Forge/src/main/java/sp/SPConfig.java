package sp;

import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Config(modid = SPMod.MODID)
@Mod.EventBusSubscriber(modid = SPMod.MODID)
public class SPConfig {

    @Config.Comment("Maximum number of particles allowed to render.")
    @Config.RangeInt(min = 0)
    public static int particleLimit = 5000;

    @Config.Comment("Enable smart camera frustum culling. Particles outside the screen view will be removed first.")
    public static boolean smartCameraCulling = true;

    @SubscribeEvent
    public static void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
        if (event.getModID().equals(SPMod.MODID)) {
            ConfigManager.sync(SPMod.MODID, Config.Type.INSTANCE);
        }
    }
}