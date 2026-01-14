package sp.mixin;

import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleRenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(ParticleEngine.class)
public interface ParticleRendererAccessor {
    @Accessor("particles")
    Map<ParticleRenderType, Object> smartParticles$getParticles();
}
