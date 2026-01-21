package sp.mixin;

import sp.SPConfig;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleGroup;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
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

    @Shadow
    private Object2IntOpenHashMap<ParticleGroup> trackedParticleCounts;

    private Particle[] spHeapParticles;
    private double[] spHeapScores;
    private Set<Particle> spKeep;

    @Inject(method = "tick", at = @At("TAIL"))
    private void smartparticles$enforceParticleLimit(CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        if (player == null) return;

        int limit = Math.max(0, SPConfig.instance.particleLimit);
        boolean smartCulling = SPConfig.instance.smartCameraCulling;

        ClientLevel level = client.level;
        double protectionThresholdSq = (level != null && (level.isRaining() || level.isThundering())) ? 512.0 : 25.0;

        if (!smartCulling) {
            int total = 0;
            for (Queue<Particle> q : particles.values()) {
                total += q.size();
            }
            if (total <= limit) return;
        }

        net.minecraft.client.Camera camera = client.gameRenderer.getMainCamera();
        Vec3 camPos = camera.getPosition();
        Vector3f camDir = camera.getLookVector();

        double fov = client.options.fov().get();
        double frustumThreshold = Math.cos(Math.toRadians((fov / 2.0) + 30.0));
        double frustumPenalty = 1.0e10;

        final double px = player.getX();
        final double py = player.getY();
        final double pz = player.getZ();

        if (limit == 0) {
            for (Queue<Particle> q : particles.values()) {
                if (q == null || q.isEmpty()) continue;

                Iterator<Particle> it = q.iterator();
                while (it.hasNext()) {
                    Particle p = it.next();
                    it.remove();
                    p.remove();
                    decrementGroupCount(p);
                }
            }
            return;
        }

        if (this.spHeapParticles == null || this.spHeapParticles.length < limit) {
            this.spHeapParticles = new Particle[limit];
            this.spHeapScores = new double[limit];
            this.spKeep = Collections.newSetFromMap(new IdentityHashMap<>(limit));
        } else {
            this.spKeep.clear();
        }

        final Particle[] heapParticles = this.spHeapParticles;
        final double[] heapScores = this.spHeapScores;
        int heapSize = 0;

        boolean dirty = false;

        for (Queue<Particle> q : particles.values()) {
            if (q == null || q.isEmpty()) continue;

            Iterator<Particle> it = q.iterator();
            while (it.hasNext()) {
                Particle p = it.next();
                SPAccessor acc = (SPAccessor) p;

                double dx = acc.smartparticles$getX() - px;
                double dy = acc.smartparticles$getY() - py;
                double dz = acc.smartparticles$getZ() - pz;
                double distSq = dx * dx + dy * dy + dz * dz;

                boolean protectedParticle = distSq <= protectionThresholdSq;
                boolean inFrustum = false;

                if (!protectedParticle) {
                    double ex = acc.smartparticles$getX() - camPos.x;
                    double ey = acc.smartparticles$getY() - camPos.y;
                    double ez = acc.smartparticles$getZ() - camPos.z;

                    double dot = ex * camDir.x + ey * camDir.y + ez * camDir.z;

                    if (dot > 0) {
                        double eDistSq = ex * ex + ey * ey + ez * ez;
                        if (dot * dot > frustumThreshold * frustumThreshold * eDistSq) {
                            inFrustum = true;
                        }
                    }
                }

                if (smartCulling && !inFrustum && !protectedParticle) {
                    it.remove();
                    p.remove();
                    decrementGroupCount(p);
                    continue;
                }

                double score = distSq;
                if (!smartCulling && !inFrustum && !protectedParticle) {
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
                    dirty = true;
                } else {
                    it.remove();
                    p.remove();
                    decrementGroupCount(p);
                }
            }
        }

        if (!dirty) return;

        Set<Particle> keep = this.spKeep;
        for (int i = 0; i < heapSize; i++) {
            keep.add(heapParticles[i]);
        }

        for (Queue<Particle> q : particles.values()) {
            if (q == null || q.isEmpty()) continue;

            Iterator<Particle> it = q.iterator();
            while (it.hasNext()) {
                Particle p = it.next();
                if (!keep.contains(p)) {
                    it.remove();
                    p.remove();
                    decrementGroupCount(p);
                }
            }
        }
    }

    private void decrementGroupCount(Particle p) {
        if (p.getParticleGroup().isPresent()) {
            ParticleGroup group = p.getParticleGroup().get();
            int current = trackedParticleCounts.getInt(group);
            if (current <= 1) {
                trackedParticleCounts.removeInt(group);
            } else {
                trackedParticleCounts.put(group, current - 1);
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
