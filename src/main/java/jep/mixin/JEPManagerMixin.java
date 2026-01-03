package com.chedidandrew.particlecap.mixin;

import com.chedidandrew.particlecap.ParticleCapConfig;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.client.particle.ParticleTextureSheet;
import net.minecraft.particle.ParticleGroup;
import net.minecraft.util.math.Vec3d;
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

@Mixin(ParticleManager.class)
public abstract class ParticleManagerMixin {

    @Shadow
    private Map<ParticleTextureSheet, Queue<Particle>> particles;

    @Shadow
    private Object2IntOpenHashMap<ParticleGroup> groupCounts;

    @Inject(method = "tick", at = @At("TAIL"))
    private void particlecap$enforceParticleLimit(CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player == null) return;

        int limit = Math.max(0, ParticleCapConfig.instance.particleLimit);
        boolean strictCulling = ParticleCapConfig.instance.strictCameraCulling;

        // If not using strict culling, we can optimize by only running when the limit is exceeded.
        if (!strictCulling) {
            int total = 0;
            for (Queue<Particle> q : particles.values()) {
                total += q.size();
            }
            if (total <= limit) return;
        }

        // Logic to calculate frustum culling parameters
        Vec3d camPos = player.getEyePos();
        Vec3d camDir = player.getRotationVec(1.0F);
        // Get FOV and add a buffer (e.g., 30 degrees) to prevent popping at screen edges
        double fov = client.options.getFov().getValue();
        double frustumThreshold = Math.cos(Math.toRadians((fov / 2.0) + 30.0));
        // Penalty for being outside the frustum (effectively "infinite" distance)
        double frustumPenalty = 1.0e10;

        final double px = player.getX();
        final double py = player.getY();
        final double pz = player.getZ();

        final Particle[] heapParticles = new Particle[limit];
        final double[] heapScores = new double[limit];
        int heapSize = 0;

        for (Queue<Particle> q : particles.values()) {
            for (Particle p : q) {
                ParticleAccessor acc = (ParticleAccessor) p;
                
                // 1. Frustum Check: Is the particle visible?
                double ex = acc.particlecap$getX() - camPos.x;
                double ey = acc.particlecap$getY() - camPos.y;
                double ez = acc.particlecap$getZ() - camPos.z;
                
                double dot = ex * camDir.x + ey * camDir.y + ez * camDir.z;
                boolean inFrustum = false;

                // Check if in front of camera (dot > 0) and within the FOV cone
                if (dot > 0) {
                     double eDistSq = ex * ex + ey * ey + ez * ez;
                     if (dot * dot > frustumThreshold * frustumThreshold * eDistSq) {
                         inFrustum = true;
                     }
                }

                // If strict culling is enabled, completely ignore/remove invisible particles
                if (strictCulling && !inFrustum) continue;

                // 2. Score Calculation
                double dx = acc.particlecap$getX() - px;
                double dy = acc.particlecap$getY() - py;
                double dz = acc.particlecap$getZ() - pz;
                double distSq = dx * dx + dy * dy + dz * dz;

                double score = distSq;
                // If standard culling (not strict), use penalty to prioritize keeping visible particles
                if (!strictCulling && !inFrustum) {
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
            Iterator<Particle> it = q.iterator();
            while (it.hasNext()) {
                Particle p = it.next();
                if (!keep.contains(p)) {
                    it.remove();
                    p.markDead();
                    decrementGroupCount(p);
                }
            }
        }
    }

    private void decrementGroupCount(Particle p) {
        p.getGroup().ifPresent(group -> {
            int current = groupCounts.getInt(group);
            if (current <= 1) {
                groupCounts.removeInt(group);
            } else {
                groupCounts.put(group, current - 1);
            }
        });
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