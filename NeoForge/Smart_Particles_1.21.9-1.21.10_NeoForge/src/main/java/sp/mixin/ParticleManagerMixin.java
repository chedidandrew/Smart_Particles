package sp.mixin;

import sp.SPConfig;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleGroup;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleLimit;
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
    private Map<ParticleRenderType, ParticleGroup<?>> particles;

    @Shadow
    private Object2IntOpenHashMap<ParticleLimit> trackedParticleCounts;

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
            for (ParticleGroup<?> g : particles.values()) {
                if (g != null) total += g.size();
            }
            if (total <= limit) return;
        }

        net.minecraft.client.Camera camera = client.gameRenderer.getMainCamera();
        Vec3 camPos = camera.getPosition();
        Vec3 camDir = Vec3.directionFromRotation(camera.getXRot(), camera.getYRot());

        double fov = client.options.fov().get();
        double frustumThreshold = Math.cos(Math.toRadians((fov / 2.0) + 30.0));
        double frustumPenalty = 1.0e10;

        final double px = player.getX();
        final double py = player.getY();
        final double pz = player.getZ();

        if (limit == 0) {
            for (ParticleGroup<?> g : particles.values()) {
                if (g == null) continue;
                Queue<? extends Particle> q = g.getAll();
                if (q == null || q.isEmpty()) continue;

                Iterator<? extends Particle> it = q.iterator();
                while (it.hasNext()) {
                    Particle p = (Particle) it.next();
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

        for (ParticleGroup<?> g : particles.values()) {
            if (g == null) continue;
            Queue<? extends Particle> q = g.getAll();
            if (q == null || q.isEmpty()) continue;

            Iterator<? extends Particle> it = q.iterator();
            while (it.hasNext()) {
                Particle p = (Particle) it.next();
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

        for (ParticleGroup<?> g : particles.values()) {
            if (g == null) continue;
            Queue<? extends Particle> q = g.getAll();
            if (q == null || q.isEmpty()) continue;

            Iterator<? extends Particle> it = q.iterator();
            while (it.hasNext()) {
                Particle p = (Particle) it.next();
                if (!keep.contains(p)) {
                    it.remove();
                    p.remove();
                    decrementGroupCount(p);
                }
            }
        }
    }

    private void decrementGroupCount(Particle p) {
        p.getParticleLimit().ifPresent(limit -> {
            int current = trackedParticleCounts.getInt(limit);
            if (current <= 1) {
                trackedParticleCounts.removeInt(limit);
            } else {
                trackedParticleCounts.put(limit, current - 1);
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
