package sp.mixin;

import net.minecraft.client.particle.Particle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Particle.class)
public interface SPAccessor {
    @Accessor(value = "x")
    double smartparticles$getX();

    @Accessor(value = "y")
    double smartparticles$getY();

    @Accessor(value = "z")
    double smartparticles$getZ();
}