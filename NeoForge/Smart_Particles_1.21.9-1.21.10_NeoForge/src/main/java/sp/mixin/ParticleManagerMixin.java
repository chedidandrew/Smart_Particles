package sp.mixin;

import sp.SPConfig;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

@Mixin(ParticleEngine.class)
public abstract class ParticleManagerMixin {

    @Inject(method = "tick", at = @At("TAIL"))
    private void smartparticles$enforceParticleLimit(CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        if (player == null) return;

        int limit = Math.max(0, SPConfig.get().particleLimit);
        boolean smartCulling = SPConfig.get().smartCameraCulling;

        final Map<ParticleRenderType, Object> particles =
                ((ParticleRendererAccessor) (Object) this).smartParticles$getParticles();

        if (!smartCulling) {
            int total = 0;
            for (Object groupObj : particles.values()) {
                if (groupObj == null) continue;

                if (groupObj instanceof Collection<?> c) {
                    total += c.size();
                } else {
                    Iterable<Particle> it = asParticleIterable(groupObj);
                    if (it != null) {
                        for (Iterator<Particle> iter = it.iterator(); iter.hasNext(); ) {
                            iter.next();
                            total++;
                        }
                    }
                }
            }
            if (total <= limit) return;
        }

        Camera camera = client.gameRenderer.getMainCamera();
        Vec3 camPos = camera.getPosition();
        Vec3 camDir = Vec3.directionFromRotation(camera.getXRot(), camera.getYRot());

        double fov = client.options.fov().get();
        double frustumThreshold = Math.cos(Math.toRadians((fov / 2.0) + 30.0));
        double frustumPenalty = 1.0e10;

        final double px = player.getX();
        final double py = player.getY();
        final double pz = player.getZ();

        final Particle[] heapParticles = new Particle[limit];
        final double[] heapScores = new double[limit];
        int heapSize = 0;

        if (limit > 0) {
            for (Object groupObj : particles.values()) {
                if (groupObj == null) continue;

                Iterable<Particle> group = asParticleIterable(groupObj);
                if (group == null) continue;

                for (Particle p : group) {
                    if (p == null) continue;

                    SPAccessor acc = (SPAccessor) p;

                    double dx = acc.smartparticles$getX() - px;
                    double dy = acc.smartparticles$getY() - py;
                    double dz = acc.smartparticles$getZ() - pz;
                    double distSq = dx * dx + dy * dy + dz * dz;

                    boolean protectedParticle = distSq <= 16.0;

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

                    if (smartCulling && !inFrustum && !protectedParticle) continue;

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
                    }
                }
            }
        }

        Set<Particle> keep = Collections.newSetFromMap(new IdentityHashMap<>());
        for (int i = 0; i < heapSize; i++) {
            keep.add(heapParticles[i]);
        }

        for (Object groupObj : particles.values()) {
            if (groupObj == null) continue;

            Iterable<Particle> group = asParticleIterable(groupObj);
            if (group == null) continue;

            Iterator<Particle> it = group.iterator();
            while (it.hasNext()) {
                Particle p = it.next();
                if (!keep.contains(p)) {
                    try {
                        it.remove();
                    } catch (UnsupportedOperationException ignored) {
                    } catch (IllegalStateException ignored) {
                    }
                    p.remove();
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static Iterable<Particle> asParticleIterable(Object groupObj) {
        if (groupObj == null) return null;

        if (groupObj instanceof Iterable<?> it) {
            return (Iterable<Particle>) it;
        }

        try {
            Method m = groupObj.getClass().getMethod("getAll");
            Object all = m.invoke(groupObj);
            if (all instanceof Iterable<?> it2) {
                return (Iterable<Particle>) it2;
            }
        } catch (Throwable ignored) {
        }

        return null;
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
