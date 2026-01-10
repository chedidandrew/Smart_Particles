package sp.mixin;

import sp.SPConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;
import java.util.*;

@Mixin(ParticleEngine.class)
public abstract class ParticleManagerMixin {

    @Shadow
    private Map<ParticleRenderType, Object> particles;

    private static final Map<Class<?>, Field> FIELD_CACHE = new HashMap<>();

    @Inject(method = "tick", at = @At("TAIL"))
    private void smartparticles$enforceParticleLimit(CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        if (player == null) return;

        int limit = Math.max(0, SPConfig.instance.particleLimit);
        boolean smartCulling = SPConfig.instance.smartCameraCulling;

        // --- 1. Count Total Particles ---
        if (!smartCulling) {
            int total = 0;
            for (Object obj : particles.values()) {
                List<Particle> list = asParticleList(obj);
                if (list != null) total += list.size();
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

        if (limit > 0) {
            // --- 2. Iterate & Score Particles ---
            for (Object obj : particles.values()) {
                List<Particle> list = asParticleList(obj);
                if (list == null) continue;

                // Use index loop to avoid ConcurrentModificationException
                for (int i = 0; i < list.size(); i++) {
                    Particle p = list.get(i);
                    if (p == null) continue;

                    // Cast to Accessor to get coordinates efficiently
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
                    if (!smartCulling && !inFrustum) score += frustumPenalty;

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

        // --- 3. Mark Kept Particles ---
        Set<Particle> keep = Collections.newSetFromMap(new IdentityHashMap<>());
        for (int i = 0; i < heapSize; i++) {
            keep.add(heapParticles[i]);
        }

        // --- 4. Remove Unkept Particles ---
        for (Object obj : particles.values()) {
            List<Particle> list = asParticleList(obj);
            if (list != null) {
                // Safe removal loop
                for (int i = 0; i < list.size(); i++) {
                    Particle p = list.get(i);
                    if (p != null && !keep.contains(p)) {
                        p.remove(); 
                    }
                }
            }
        }
    }

    /**
     * Extracts a List<Particle> by searching the object AND its parent classes.
     * This fixes the issue where the list was hidden in the parent 'ParticleGroup'.
     */
    @SuppressWarnings("unchecked")
    private List<Particle> asParticleList(Object obj) {
        if (obj == null) return null;

        // Fast Path: Standard Java Collections
        if (obj instanceof Collection) {
            if (obj instanceof List) return (List<Particle>) obj;
            return new ArrayList<>((Collection<Particle>) obj);
        }

        Class<?> clazz = obj.getClass();
        
        // 1. Check Cache
        Field cached = FIELD_CACHE.get(clazz);
        if (cached != null) {
            try { return (List<Particle>) cached.get(obj); } catch (Exception ignored) { return null; }
        }

        // 2. Search Hierarchy (The Fix!)
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            for (Field f : current.getDeclaredFields()) {
                if (List.class.isAssignableFrom(f.getType())) {
                    f.setAccessible(true);
                    FIELD_CACHE.put(clazz, f); // Cache for the original class
                    try { return (List<Particle>) f.get(obj); } catch (Exception ignored) { return null; }
                }
            }
            current = current.getSuperclass(); // Move up to parent (ParticleGroup)
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
            if (right < size && ds[right] > ds[left]) largest = right;
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