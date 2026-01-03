package sp;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.text.Text;

public class SPModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> {
            ConfigBuilder builder = ConfigBuilder.create()
                    .setParentScreen(parent)
                    .setTitle(Text.of("Smart Particles Config"));

            ConfigCategory general = builder.getOrCreateCategory(Text.of("General"));
            ConfigEntryBuilder entryBuilder = builder.entryBuilder();

            general.addEntry(entryBuilder.startIntField(Text.of("Particle Limit"), SPConfig.instance.particleLimit)
                    .setDefaultValue(5000)
                    .setMin(0)
                    .setTooltip(Text.of("The maximum number of particles allowed."))
                    .setSaveConsumer(newValue -> SPConfig.instance.particleLimit = newValue)
                    .build());

            general.addEntry(entryBuilder.startBooleanToggle(Text.of("Smart Camera Culling"), SPConfig.instance.smartCameraCulling)
                    .setDefaultValue(true)
                    .setTooltip(Text.of("Aggressively removes particles outside the camera view, even if below the limit."))
                    .setSaveConsumer(newValue -> SPConfig.instance.smartCameraCulling = newValue)
                    .build());

            builder.setSavingRunnable(SPConfig::save);

            return builder.build();
        };
    }
}