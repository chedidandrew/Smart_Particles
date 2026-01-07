package sp;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import java.lang.reflect.Field;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Set;

@Mod(modid = SPMod.MODID, name = SPMod.NAME, version = SPMod.VERSION, clientSideOnly = true)
public class SPMod {
    public static final String MODID = "smartparticles";
    public static final String NAME = "Smart Particles";
    public static final String VERSION = "12.02.7";

    // Reflection Fields
    private static final String[] FX_LAYERS_NAMES = new String[]{"fxLayers", "field_78876_b"};
    
    private static Field fieldPosX;
    private static Field fieldPosY;
    private static Field fieldPosZ;

    static {
        try {
            // FIX: Explicitly use new String[]{} to force the correct Varargs method signature
            // This prevents the NoSuchMethodError on older Forge versions (like RLCraft's)
            fieldPosX = ReflectionHelper.findField(Particle.class, new String[]{"posX", "field_187126_f"});
            fieldPosY = ReflectionHelper.findField(Particle.class, new String[]{"posY", "field_187127_g"});
            fieldPosZ = ReflectionHelper.findField(Particle.class, new String[]{"posZ", "field_187128_h"});
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        
        Minecraft client = Minecraft.getMinecraft();
        if (client.isGamePaused() || client.player == null || client.effectRenderer == null) return;

        performCulling(client);
    }

    private void performCulling(Minecraft client) {
        int limit = Math.max(0, SPConfig.particleLimit);
        boolean smartCulling = SPConfig.smartCameraCulling;

        // Get the list of particles
        ArrayDeque<Particle>[][] fxLayers;
        try {
            fxLayers = ReflectionHelper.getPrivateValue(ParticleManager.class, client.effectRenderer, FX_LAYERS_NAMES);
        } catch (Exception e) {
            return; 
        }

        // 1. Calculate total
        if (!smartCulling) {
            int total = 0;
            for (ArrayDeque<Particle>[] layer : fxLayers) {
                for (ArrayDeque<Particle> queue : layer) {
                    total += queue.size();
                }
            }
            if (total <= limit) return;
        }

        EntityPlayerSP player = client.player;
        Vec3d camPos = player.getPositionEyes(1.0F);
        Vec3d camDir = player.getLook(1.0F);
        
        double fov = client.gameSettings.fovSetting;
        double frustumThreshold = Math.cos(Math.toRadians((fov / 2.0) + 30.0));
        double frustumPenalty = 1.0e10;

        final double px = player.posX;
        final double py = player.posY;
        final double pz = player.posZ;

        final Particle[] heapParticles = new Particle[limit];
        final double[] heapScores = new double[limit];
        int heapSize = 0;

        // Iterate particles
        for (ArrayDeque<Particle>[] layer : fxLayers) {
            for (ArrayDeque<Particle> queue : layer) {
                for (Particle p : queue) {
                    try {
                        // Use Reflection to read the protected coordinates
                        double pX = fieldPosX.getDouble(p);
                        double pY = fieldPosY.getDouble(p);
                        double pZ = fieldPosZ.getDouble(p);

                        // Frustum Check
                        double ex = pX - camPos.x;
                        double ey = pY - camPos.y;
                        double ez = pZ - camPos.z;

                        double dot = ex * camDir.x + ey * camDir.y + ez * camDir.z;
                        boolean inFrustum = false;

                        if (dot > 0) {
                             double eDistSq = ex * ex + ey * ey + ez * ez;
                             if (dot * dot > frustumThreshold * frustumThreshold * eDistSq) {
                                 inFrustum = true;
                             }
                        }

                        if (smartCulling && !inFrustum) continue;

                        double dx = pX - px;
                        double dy = pY - py;
                        double dz = pZ - pz;
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
                    } catch (IllegalAccessException e) {
                        // Should not happen if reflection setup worked
                    }
                }
            }
        }

        // Cleanup
        Set<Particle> keep = Collections.newSetFromMap(new IdentityHashMap<>());
        for (int i = 0; i < heapSize; i++) {
            keep.add(heapParticles[i]);
        }

        for (ArrayDeque<Particle>[] layer : fxLayers) {
            for (ArrayDeque<Particle> queue : layer) {
                Iterator<Particle> it = queue.iterator();
                while (it.hasNext()) {
                    Particle p = it.next();
                    if (!keep.contains(p)) {
                        it.remove();
                        p.setExpired();
                    }
                }
            }
        }
    }

    // Heap Helpers
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