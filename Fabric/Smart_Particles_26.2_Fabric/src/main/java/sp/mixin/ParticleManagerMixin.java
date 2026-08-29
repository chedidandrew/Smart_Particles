package sp.mixin;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleGroup;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleLimit;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import sp.SPConfig;

import java.util.Arrays;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

@Mixin(ParticleEngine.class)
public abstract class ParticleManagerMixin {
    @Unique
    private static final double SMARTPARTICLES_CLEAR_PROTECTION_DISTANCE_SQ = 25.0;
    @Unique
    private static final double SMARTPARTICLES_WEATHER_PROTECTION_DISTANCE_SQ = 512.0;
    @Unique
    private static final double SMARTPARTICLES_OFFSCREEN_PENALTY = 1.0e10;

    @Shadow
    @Final
    private Map<ParticleRenderType, ParticleGroup<?>> particles;

    @Shadow
    @Final
    private Object2IntOpenHashMap<ParticleLimit> trackedParticleCounts;

    @Unique
    private Particle[] smartparticles$heapParticles;
    @Unique
    private double[] smartparticles$heapScores;
    @Unique
    private Set<Particle> smartparticles$keep;
    @Unique
    private final Vector3f smartparticles$forward = new Vector3f();
    @Unique
    private double smartparticles$cachedFov = Double.NaN;
    @Unique
    private double smartparticles$cachedFrustumThresholdSq;

    @Inject(method = "tick", at = @At("TAIL"))
    private void smartparticles$enforceParticleLimit(CallbackInfo callbackInfo) {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        if (player == null) {
            return;
        }

        int total = smartparticles$countParticles();
        if (total == 0) {
            return;
        }

        SPConfig config = SPConfig.get();
        int limit = SPConfig.clampParticleLimit(config.particleLimit);
        boolean smartCulling = config.smartCameraCulling;

        if (limit == 0) {
            smartparticles$clearAllParticles();
            smartparticles$releaseOversizedBuffers(0);
            return;
        }

        if (!smartCulling && total <= limit) {
            smartparticles$releaseOversizedBuffers(limit);
            return;
        }

        Camera camera = client.gameRenderer.getMainCamera();
        Vec3 cameraPosition = camera.position();
        Vector3f forward = this.smartparticles$forward.set(0.0F, 0.0F, -1.0F).rotate(camera.rotation());
        double directionX = forward.x();
        double directionY = forward.y();
        double directionZ = forward.z();

        double fov = client.options.fov().get();
        if (Double.compare(fov, this.smartparticles$cachedFov) != 0) {
            double threshold = Math.cos(Math.toRadians((fov / 2.0) + 30.0));
            this.smartparticles$cachedFov = fov;
            this.smartparticles$cachedFrustumThresholdSq = threshold * threshold;
        }

        double playerX = player.getX();
        double playerY = player.getY();
        double playerZ = player.getZ();
        double protectionDistanceSq = client.level != null
                && (client.level.isRaining() || client.level.isThundering())
                ? SMARTPARTICLES_WEATHER_PROTECTION_DISTANCE_SQ
                : SMARTPARTICLES_CLEAR_PROTECTION_DISTANCE_SQ;

        if (smartCulling && total <= limit) {
            smartparticles$cullOutsideCamera(
                    playerX, playerY, playerZ,
                    cameraPosition.x, cameraPosition.y, cameraPosition.z,
                    directionX, directionY, directionZ,
                    protectionDistanceSq, this.smartparticles$cachedFrustumThresholdSq
            );
            smartparticles$releaseOversizedBuffers(limit);
            return;
        }

        smartparticles$selectBestParticles(
                limit, smartCulling,
                playerX, playerY, playerZ,
                cameraPosition.x, cameraPosition.y, cameraPosition.z,
                directionX, directionY, directionZ,
                protectionDistanceSq, this.smartparticles$cachedFrustumThresholdSq
        );
    }

    @Unique
    private int smartparticles$countParticles() {
        int total = 0;
        for (ParticleGroup<?> group : this.particles.values()) {
            if (group != null) {
                total += group.size();
            }
        }
        return total;
    }

    @Unique
    private void smartparticles$clearAllParticles() {
        for (ParticleGroup<?> group : this.particles.values()) {
            if (group == null || group.isEmpty()) {
                continue;
            }

            Queue<? extends Particle> queue = group.getAll();
            Iterator<? extends Particle> iterator = queue.iterator();
            while (iterator.hasNext()) {
                Particle particle = iterator.next();
                iterator.remove();
                if (particle != null) {
                    particle.remove();
                    smartparticles$decrementGroupCount(particle);
                }
            }
        }
    }

    @Unique
    private void smartparticles$cullOutsideCamera(
            double playerX, double playerY, double playerZ,
            double cameraX, double cameraY, double cameraZ,
            double directionX, double directionY, double directionZ,
            double protectionDistanceSq, double frustumThresholdSq
    ) {
        for (ParticleGroup<?> group : this.particles.values()) {
            if (group == null || group.isEmpty()) {
                continue;
            }

            Iterator<? extends Particle> iterator = group.getAll().iterator();
            while (iterator.hasNext()) {
                Particle particle = iterator.next();
                if (particle == null) {
                    iterator.remove();
                    continue;
                }

                SPAccessor accessor = (SPAccessor) particle;
                double x = accessor.smartparticles$getX();
                double y = accessor.smartparticles$getY();
                double z = accessor.smartparticles$getZ();

                double dx = x - playerX;
                double dy = y - playerY;
                double dz = z - playerZ;
                double distanceSq = dx * dx + dy * dy + dz * dz;

                if (distanceSq > protectionDistanceSq
                        && !smartparticles$isInsideCameraCone(
                        x, y, z,
                        cameraX, cameraY, cameraZ,
                        directionX, directionY, directionZ,
                        frustumThresholdSq)) {
                    iterator.remove();
                    particle.remove();
                    smartparticles$decrementGroupCount(particle);
                }
            }
        }
    }

    @Unique
    private void smartparticles$selectBestParticles(
            int limit, boolean smartCulling,
            double playerX, double playerY, double playerZ,
            double cameraX, double cameraY, double cameraZ,
            double directionX, double directionY, double directionZ,
            double protectionDistanceSq, double frustumThresholdSq
    ) {
        smartparticles$ensureBufferCapacity(limit);
        Particle[] heapParticles = this.smartparticles$heapParticles;
        double[] heapScores = this.smartparticles$heapScores;
        int heapSize = 0;
        boolean replacedHeapEntry = false;

        for (ParticleGroup<?> group : this.particles.values()) {
            if (group == null || group.isEmpty()) {
                continue;
            }

            Iterator<? extends Particle> iterator = group.getAll().iterator();
            while (iterator.hasNext()) {
                Particle particle = iterator.next();
                if (particle == null) {
                    iterator.remove();
                    continue;
                }

                SPAccessor accessor = (SPAccessor) particle;
                double x = accessor.smartparticles$getX();
                double y = accessor.smartparticles$getY();
                double z = accessor.smartparticles$getZ();

                double dx = x - playerX;
                double dy = y - playerY;
                double dz = z - playerZ;
                double distanceSq = dx * dx + dy * dy + dz * dz;
                boolean protectedParticle = distanceSq <= protectionDistanceSq;
                boolean insideCamera = protectedParticle || smartparticles$isInsideCameraCone(
                        x, y, z,
                        cameraX, cameraY, cameraZ,
                        directionX, directionY, directionZ,
                        frustumThresholdSq
                );

                if (smartCulling && !insideCamera) {
                    iterator.remove();
                    particle.remove();
                    smartparticles$decrementGroupCount(particle);
                    continue;
                }

                double score = distanceSq;
                if (!smartCulling && !insideCamera) {
                    score += SMARTPARTICLES_OFFSCREEN_PENALTY;
                }

                if (heapSize < limit) {
                    heapParticles[heapSize] = particle;
                    heapScores[heapSize] = score;
                    smartparticles$heapSiftUp(heapParticles, heapScores, heapSize);
                    heapSize++;
                } else if (score < heapScores[0]) {
                    heapParticles[0] = particle;
                    heapScores[0] = score;
                    smartparticles$heapSiftDown(heapParticles, heapScores, heapSize, 0);
                    replacedHeapEntry = true;
                } else {
                    iterator.remove();
                    particle.remove();
                    smartparticles$decrementGroupCount(particle);
                }
            }
        }

        if (replacedHeapEntry) {
            Set<Particle> keep = this.smartparticles$keep;
            for (int index = 0; index < heapSize; index++) {
                keep.add(heapParticles[index]);
            }

            for (ParticleGroup<?> group : this.particles.values()) {
                if (group == null || group.isEmpty()) {
                    continue;
                }

                Iterator<? extends Particle> iterator = group.getAll().iterator();
                while (iterator.hasNext()) {
                    Particle particle = iterator.next();
                    if (particle == null) {
                        iterator.remove();
                    } else if (!keep.contains(particle)) {
                        iterator.remove();
                        particle.remove();
                        smartparticles$decrementGroupCount(particle);
                    }
                }
            }
            keep.clear();
        }

        Arrays.fill(heapParticles, 0, heapSize, null);
    }

    @Unique
    private static boolean smartparticles$isInsideCameraCone(
            double particleX, double particleY, double particleZ,
            double cameraX, double cameraY, double cameraZ,
            double directionX, double directionY, double directionZ,
            double frustumThresholdSq
    ) {
        double x = particleX - cameraX;
        double y = particleY - cameraY;
        double z = particleZ - cameraZ;
        double dot = x * directionX + y * directionY + z * directionZ;
        if (dot <= 0.0) {
            return false;
        }

        double distanceSq = x * x + y * y + z * z;
        return dot * dot > frustumThresholdSq * distanceSq;
    }

    @Unique
    private void smartparticles$ensureBufferCapacity(int limit) {
        int shrinkThreshold = Math.max(8192, limit * 4);
        if (this.smartparticles$heapParticles == null
                || this.smartparticles$heapParticles.length < limit
                || this.smartparticles$heapParticles.length > shrinkThreshold) {
            this.smartparticles$heapParticles = new Particle[limit];
            this.smartparticles$heapScores = new double[limit];
            this.smartparticles$keep = Collections.newSetFromMap(new IdentityHashMap<>(limit));
        } else {
            this.smartparticles$keep.clear();
        }
    }

    @Unique
    private void smartparticles$releaseOversizedBuffers(int limit) {
        if (this.smartparticles$heapParticles == null) {
            return;
        }

        int shrinkThreshold = Math.max(8192, limit * 4);
        if (this.smartparticles$heapParticles.length > shrinkThreshold) {
            Arrays.fill(this.smartparticles$heapParticles, null);
            this.smartparticles$heapParticles = null;
            this.smartparticles$heapScores = null;
            if (this.smartparticles$keep != null) {
                this.smartparticles$keep.clear();
                this.smartparticles$keep = null;
            }
        }
    }

    @Unique
    private void smartparticles$decrementGroupCount(Particle particle) {
        particle.getParticleLimit().ifPresent(limit -> {
            int current = this.trackedParticleCounts.getInt(limit);
            if (current <= 1) {
                this.trackedParticleCounts.removeInt(limit);
            } else {
                this.trackedParticleCounts.put(limit, current - 1);
            }
        });
    }

    @Unique
    private static void smartparticles$heapSiftUp(Particle[] particles, double[] scores, int index) {
        while (index > 0) {
            int parent = (index - 1) >>> 1;
            if (scores[parent] >= scores[index]) {
                return;
            }
            smartparticles$swap(particles, scores, parent, index);
            index = parent;
        }
    }

    @Unique
    private static void smartparticles$heapSiftDown(
            Particle[] particles, double[] scores, int size, int index
    ) {
        while (true) {
            int left = (index << 1) + 1;
            if (left >= size) {
                return;
            }

            int right = left + 1;
            int largest = right < size && scores[right] > scores[left] ? right : left;
            if (scores[index] >= scores[largest]) {
                return;
            }

            smartparticles$swap(particles, scores, index, largest);
            index = largest;
        }
    }

    @Unique
    private static void smartparticles$swap(Particle[] particles, double[] scores, int first, int second) {
        Particle particle = particles[first];
        particles[first] = particles[second];
        particles[second] = particle;

        double score = scores[first];
        scores[first] = scores[second];
        scores[second] = score;
    }
}
