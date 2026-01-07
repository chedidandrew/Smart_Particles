package sp.mixin;

import net.minecraft.client.particle.ParticleEngine;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ParticleEngine.class)
public interface ParticleRendererAccessor {
}