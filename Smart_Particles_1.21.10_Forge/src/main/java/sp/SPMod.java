package sp;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(SPMod.MODID)
public class SPMod {
    public static final String MODID = "smart_particles";

    public SPMod(FMLJavaModLoadingContext context) {
        MinecraftForge.registerConfigScreen(SPConfigScreen::new);
        SPConfig.load();
    }
}
