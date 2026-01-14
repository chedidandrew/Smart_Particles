package sp.mixin;

import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleRenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

/**
 * In 1.21.9 this map's value type is not a Queue, so keep it as Object and handle dynamically.
 */
@Mixin(ParticleEngine.class)
public interface ParticleRendererAccessor {
    @Accessor("particles")
    Map<ParticleRenderType, Object> smartParticles$getParticles();
}
