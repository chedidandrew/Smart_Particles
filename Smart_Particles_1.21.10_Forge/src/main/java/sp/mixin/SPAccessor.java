package sp.mixin;

import net.minecraft.client.particle.Particle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Particle.class)
public interface SPAccessor {
    @Accessor(value = "x", remap = false)
    double smartparticles$getX();

    @Accessor(value = "y", remap = false)
    double smartparticles$getY();

    @Accessor(value = "z", remap = false)
    double smartparticles$getZ();
}