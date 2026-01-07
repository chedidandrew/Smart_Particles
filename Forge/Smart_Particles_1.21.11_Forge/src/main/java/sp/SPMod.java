package sp;

import net.minecraftforge.fml.common.Mod;

@Mod("smart_particles")
public class SPMod {
    public SPMod() {
        SPConfig.load();
    }
}