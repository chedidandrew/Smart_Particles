package com.chedidandrew.particlecap.mixin;

import net.minecraft.client.particle.Particle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Particle.class)
public interface ParticleAccessor {
	@Accessor("x")
	double particlecap$getX();

	@Accessor("y")
	double particlecap$getY();

	@Accessor("z")
	double particlecap$getZ();
}
