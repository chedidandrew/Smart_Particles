package sp;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;

@Mod(SPMod.MODID)
public class SPMod {
    public static final String MODID = "smart_particles";

    public SPMod() {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            SPConfig.load();
            MinecraftForge.registerConfigScreen(parent -> new SPConfigScreen(parent));
        }
    }
}
