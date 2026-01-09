package sp.mixin;

import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import java.util.Queue;

// Mixin into the superclass (class_11938) so it works for BillboardParticleRenderer too
@Mixin(ParticleRenderer.class)
public interface ParticleRendererAccessor {
    // Access the private 'particles' field (Intermediary: field_62620)
    @Accessor("particles") 
    Queue<Particle> sp$getParticles();
}