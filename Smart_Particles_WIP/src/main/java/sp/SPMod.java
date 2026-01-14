package sp;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(SPMod.MOD_ID)
public class SPMod {
    public static final String MOD_ID = "smart_particles";

    public SPMod(net.neoforged.fml.ModContainer modContainer) {
        // Keep your existing config persistence behavior (JSON under config/smart_particles/config.json)
        SPConfig.load();

        // Enable the Mod List "Config" button by registering a config screen factory
        if (FMLEnvironment.getDist() == Dist.CLIENT) {
            modContainer.registerExtensionPoint(IConfigScreenFactory.class,
                    (container, parent) -> new SPConfigScreen(parent));
        }
    }
}
