package sp.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import net.minecraft.client.particle.Particle;

@Mixin(Particle.class)
public interface SPAccessor {
	@Accessor("x")
	double smartparticles$getX();

	@Accessor("y")
	double smartparticles$getY();

	@Accessor("z")
	double smartparticles$getZ();
}
