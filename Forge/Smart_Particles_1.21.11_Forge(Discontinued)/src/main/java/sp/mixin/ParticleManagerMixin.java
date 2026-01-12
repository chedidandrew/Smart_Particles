package sp.mixin;

import sp.SPConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

@Mixin(ParticleEngine.class)
public abstract class ParticleManagerMixin {

    @Shadow
    private Map<ParticleRenderType, Queue<Particle>> particles;

    @Inject(method = "tick", at = @At("TAIL"))
    private void smartparticles$enforceParticleLimit(CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        if (player == null) return;

        int limit = Math.max(0, SPConfig.instance.particleLimit);
        boolean smartCulling = SPConfig.instance.smartCameraCulling;

        if (!smartCulling) {
            int total = 0;
            for (Queue<Particle> q : particles.values()) {
                total += q.size();
            }
            if (total <= limit) return;
        }

        Vec3 camPos = player.getEyePosition();
        Vec3 camDir = player.getViewVector(1.0F);
        
        double fov = client.options.fov().get();
        double frustumThreshold = Math.cos(Math.toRadians((fov / 2.0) + 30.0));
        double frustumPenalty = 1.0e10;

        final double px = player.getX();
        final double py = player.getY();
        final double pz = player.getZ();

        final Particle[] heapParticles = new Particle[limit];
        final double[] heapScores = new double[limit];
        int heapSize = 0;

        // --- 3. BUILD PRIORITY HEAP (KEEP LIST) ---
        for (Queue<Particle> q : particles.values()) {
            if (q == null) continue;

            for (Particle p : q) {
                SPAccessor acc = (SPAccessor) p;
                
                double ex = acc.smartparticles$getX() - camPos.x;
                double ey = acc.smartparticles$getY() - camPos.y;
                double ez = acc.smartparticles$getZ() - camPos.z;
                
                double dot = ex * camDir.x + ey * camDir.y + ez * camDir.z;
                boolean inFrustum = false;

                if (dot > 0) {
                     double eDistSq = ex * ex + ey * ey + ez * ez;
                     if (dot * dot > frustumThreshold * frustumThreshold * eDistSq) {
                         inFrustum = true;
                     }
                }

                if (smartCulling && !inFrustum) continue;

                double dx = acc.smartparticles$getX() - px;
                double dy = acc.smartparticles$getY() - py;
                double dz = acc.smartparticles$getZ() - pz;
                double distSq = dx * dx + dy * dy + dz * dz;

                double score = distSq;
                if (!smartCulling && !inFrustum) {
                    score += frustumPenalty;
                }

                if (heapSize < limit) {
                    heapParticles[heapSize] = p;
                    heapScores[heapSize] = score;
                    heapSiftUp(heapParticles, heapScores, heapSize);
                    heapSize++;
                } else if (score < heapScores[0]) {
                    heapParticles[0] = p;
                    heapScores[0] = score;
                    heapSiftDown(heapParticles, heapScores, heapSize, 0);
                }
            }
        }

        Set<Particle> keep = Collections.newSetFromMap(new IdentityHashMap<>());
        for (int i = 0; i < heapSize; i++) {
            keep.add(heapParticles[i]);
        }

        for (Queue<Particle> q : particles.values()) {
            if (q == null) continue;

            Iterator<Particle> it = q.iterator();
            while (it.hasNext()) {
                Particle p = it.next();
                if (!keep.contains(p)) {
                    it.remove();
                    p.remove();
                }
            }
        }
    }

    private static void heapSiftUp(Particle[] ps, double[] ds, int idx) {
        while (idx > 0) {
            int parent = (idx - 1) >>> 1;
            if (ds[parent] >= ds[idx]) return;
            swap(ps, ds, parent, idx);
            idx = parent;
        }
    }

    private static void heapSiftDown(Particle[] ps, double[] ds, int size, int idx) {
        while (true) {
            int left = (idx << 1) + 1;
            if (left >= size) return;

            int right = left + 1;
            int largest = left;

            if (right < size && ds[right] > ds[left]) {
                largest = right;
            }

            if (ds[idx] >= ds[largest]) return;

            swap(ps, ds, idx, largest);
            idx = largest;
        }
    }

    private static void swap(Particle[] ps, double[] ds, int a, int b) {
        Particle tp = ps[a];
        ps[a] = ps[b];
        ps[b] = tp;

        double td = ds[a];
        ds[a] = ds[b];
        ds[b] = td;
    }
}