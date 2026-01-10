package sp;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod("smart_particles")
public class SPMod {
    public static final String MODID = "smart_particles";

    public SPMod(ModContainer modContainer, IEventBus modBus) {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            SPConfig.load();
            
            modContainer.registerExtensionPoint(IConfigScreenFactory.class, 
                (mc, parent) -> new SPConfigScreen(parent)
            );
        }
    }
}