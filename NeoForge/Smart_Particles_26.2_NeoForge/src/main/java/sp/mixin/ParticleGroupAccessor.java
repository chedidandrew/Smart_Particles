package sp.mixin;

import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleGroup;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Queue;

@Mixin(ParticleGroup.class)
public interface ParticleGroupAccessor {
    @Accessor("particles")
    Queue<Particle> smartparticles$getParticles();
}
