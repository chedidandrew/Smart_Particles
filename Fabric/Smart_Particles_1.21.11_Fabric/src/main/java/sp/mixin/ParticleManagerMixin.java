package sp.mixin;

import sp.SPConfig;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.client.particle.ParticleRenderer;
import net.minecraft.client.particle.ParticleTextureSheet;
import net.minecraft.client.render.Camera;
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
    private Map<ParticleTextureSheet, ParticleRenderer<?>> particles;

    @Shadow
    private Object2IntOpenHashMap<ParticleGroup> groupCounts;

    private Particle[] spHeapParticles;
    private double[] spHeapScores;
    private Set<Particle> spKeep;

    @Inject(method = "tick", at = @At("TAIL"))
    private void smartparticles$enforceParticleLimit(CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player == null) return;

        int limit = Math.max(0, SPConfig.instance.particleLimit);
        boolean smartCulling = SPConfig.instance.smartCameraCulling;

        var world = client.world;
        double protectionThresholdSq = (world != null && (world.isRaining() || world.isThundering())) ? 512.0 : 25.0;

        if (!smartCulling) {
            int total = 0;
            for (ParticleRenderer<?> r : particles.values()) {
                total += r.size();
            }
            if (total <= limit) return;
        }

        Camera camera = client.gameRenderer.getCamera();
        Vec3d camPos = camera.getCameraPos();
        Vec3d camDir = Vec3d.fromPolar(camera.getPitch(), camera.getYaw());

        double fov = client.options.getFov().getValue();
        double frustumThreshold = Math.cos(Math.toRadians((fov / 2.0) + 30.0));
        double frustumPenalty = 1.0e10;

        final double px = player.getX();
        final double py = player.getY();
        final double pz = player.getZ();

        // 1. LIMIT = 0 LOGIC
        if (limit == 0) {
            for (ParticleRenderer<?> r : particles.values()) {
                Queue<? extends Particle> q = r.getParticles();
                Iterator<? extends Particle> it = q.iterator();
                while (it.hasNext()) {
                    Particle p = (Particle) it.next();
                    it.remove();
                    
                    // FIX: Null Check
                    if (p == null) continue;
                    
                    p.markDead();
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

        // 2. MAIN SCORING LOOP
        if (limit > 0) for (ParticleRenderer<?> r : particles.values()) {
            Queue<? extends Particle> qView = r.getParticles();
            @SuppressWarnings("unchecked")
            Queue<Particle> q = (Queue<Particle>) (Queue<?>) qView;

            Iterator<Particle> it = q.iterator();
            while (it.hasNext()) {
                Particle p = it.next();
                
                // FIX: Null Check (Safely remove garbage if found)
                if (p == null) {
                    it.remove();
                    continue;
                }

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
                    p.markDead();
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
                    p.markDead();
                    decrementGroupCount(p);
                }
            }
        }

        if (!dirty) return;

        Set<Particle> keep = this.spKeep;
        for (int i = 0; i < heapSize; i++) {
            keep.add(heapParticles[i]);
        }

        // 3. FINAL CLEANUP LOOP
        for (ParticleRenderer<?> r : particles.values()) {
            Queue<? extends Particle> qView = r.getParticles();
            @SuppressWarnings("unchecked")
            Queue<Particle> q = (Queue<Particle>) (Queue<?>) qView;

            Iterator<Particle> it = q.iterator();
            while (it.hasNext()) {
                Particle p = it.next();
                
                // FIX: Null Check
                if (p == null) {
                    it.remove();
                    continue;
                }

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