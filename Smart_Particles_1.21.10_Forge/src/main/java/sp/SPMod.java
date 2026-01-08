package sp;

import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;

@Mod("smart_particles")
public class SPMod {
    
    public SPMod() {
        // Register the config screen using the static context to avoid constructor errors.
        ModLoadingContext.get().registerExtensionPoint(
            ConfigScreenHandler.ConfigScreenFactory.class,
            () -> new ConfigScreenHandler.ConfigScreenFactory((mc, screen) -> new SPConfigScreen(screen))
        );
        SPConfig.load();
    }
}