package sp;

import cpw.mods.fml.client.config.GuiConfig;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.common.config.ConfigElement;
import net.minecraftforge.common.config.Configuration;

import java.util.List;

public class SPGuiConfig extends GuiConfig {
    public SPGuiConfig(GuiScreen parentScreen) {
        super(
            parentScreen,
            // 1. Get all options from the "general" category of your config
            new ConfigElement(SPConfig.config.getCategory(Configuration.CATEGORY_GENERAL)).getChildElements(),
            SPMod.MODID, 
            false, // requireWorldRestart
            false, // requireMcRestart
            SPMod.NAME + " Config" // Title
        );
    }
}