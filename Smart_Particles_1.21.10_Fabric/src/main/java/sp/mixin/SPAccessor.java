package sp.mixin;

import net.minecraft.client.particle.Particle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Particle.class)
public interface SPAccessor {
	@Accessor("x")
	double smartparticles$getX();

	@Accessor("y")
	double smartparticles$getY();

	@Accessor("z")
	double smartparticles$getZ();
}
