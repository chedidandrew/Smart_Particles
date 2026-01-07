package sp;

import cpw.mods.fml.client.event.ConfigChangedEvent;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.ReflectionHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.client.particle.EffectRenderer;
import net.minecraft.client.particle.EntityFX;
import net.minecraft.util.Vec3;

import java.util.*;

@Mod(modid = SPMod.MODID, name = SPMod.NAME, version = SPMod.VERSION, guiFactory = "sp.SPGuiFactory")
public class SPMod {
    public static final String MODID = "smartparticles";
    public static final String NAME = "Smart Particles";
    public static final String VERSION = "1.7.10-1.2";

    // "b" is the common obfuscated name for fxLayers in 1.7.10
    private static final String[] FX_LAYERS_NAMES = new String[]{"field_78876_b", "fxLayers", "b"};
    
    // Debug flag
    private static boolean hasLoggedError = false;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        SPConfig.init(event.getSuggestedConfigurationFile());
        FMLCommonHandler.instance().bus().register(this);
    }

    // THIS WAS MISSING: Allows the config to update without restarting the game
    @SubscribeEvent
    public void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
        if (event.modID.equals(MODID)) {
            SPConfig.syncConfig();
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft client = Minecraft.getMinecraft();
        if (client.isGamePaused() || client.thePlayer == null || client.effectRenderer == null) return;

        performCulling(client);
    }

    private void performCulling(Minecraft client) {
        int limit = Math.max(0, SPConfig.particleLimit);
        boolean smartCulling = SPConfig.smartCameraCulling;

        List<EntityFX>[] fxLayers;
        try {
            fxLayers = ReflectionHelper.getPrivateValue(EffectRenderer.class, client.effectRenderer, FX_LAYERS_NAMES);
        } catch (Exception e) {
            if (!hasLoggedError) {
                System.out.println("[SmartParticles] CRITICAL ERROR: Could not access particle list.");
                e.printStackTrace();
                hasLoggedError = true;
            }
            return;
        }

        // Optimization: If limit is 0, clear EVERYTHING immediately
        if (limit == 0 && !smartCulling) {
             for (List<EntityFX> layer : fxLayers) {
                 if (!layer.isEmpty()) layer.clear();
             }
             return;
        }

        // 1. Total check (if smart culling is off)
        if (!smartCulling) {
            int total = 0;
            for (List<EntityFX> layer : fxLayers) {
                total += layer.size();
            }
            if (total <= limit) return;
        }

        EntityClientPlayerMP player = client.thePlayer;
        
        // 1.7.10 Vector Helper
        Vec3 camPos = Vec3.createVectorHelper(
            player.prevPosX + (player.posX - player.prevPosX),
            player.prevPosY + (player.posY - player.prevPosY) + player.getEyeHeight(),
            player.prevPosZ + (player.posZ - player.prevPosZ)
        );
        
        Vec3 camDir = player.getLook(1.0F);

        double fov = client.gameSettings.fovSetting;
        double frustumThreshold = Math.cos(Math.toRadians((fov / 2.0) + 30.0));
        double frustumPenalty = 1.0e10;

        final double px = player.posX;
        final double py = player.posY;
        final double pz = player.posZ;

        int safeLimit = Math.max(1, limit);
        final EntityFX[] heapParticles = new EntityFX[safeLimit];
        final double[] heapScores = new double[safeLimit];
        int heapSize = 0;

        for (List<EntityFX> layer : fxLayers) {
            // Use standard for-loop with index to avoid ConcurrentModification if list changes during read
            // (Though iterating copy or standard iterator is usually safer, this is standard pattern)
            for (int i = 0; i < layer.size(); i++) {
                EntityFX p = layer.get(i);
                if (p == null) continue;

                double pX = p.posX;
                double pY = p.posY;
                double pZ = p.posZ;

                double ex = pX - camPos.xCoord;
                double ey = pY - camPos.yCoord;
                double ez = pZ - camPos.zCoord;

                double dot = ex * camDir.xCoord + ey * camDir.yCoord + ez * camDir.zCoord;
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

                if (heapSize < safeLimit) {
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

        Set<EntityFX> keep = Collections.newSetFromMap(new IdentityHashMap<EntityFX, Boolean>());
        for (int i = 0; i < heapSize; i++) {
            keep.add(heapParticles[i]);
        }

        for (List<EntityFX> layer : fxLayers) {
            Iterator<EntityFX> it = layer.iterator();
            while (it.hasNext()) {
                EntityFX p = it.next();
                if (!keep.contains(p)) {
                    it.remove();
                    p.setDead();
                }
            }
        }
    }

    private static void heapSiftUp(EntityFX[] ps, double[] ds, int idx) {
        while (idx > 0) {
            int parent = (idx - 1) >>> 1;
            if (ds[parent] >= ds[idx]) return;
            swap(ps, ds, parent, idx);
            idx = parent;
        }
    }

    private static void heapSiftDown(EntityFX[] ps, double[] ds, int size, int idx) {
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

    private static void swap(EntityFX[] ps, double[] ds, int a, int b) {
        EntityFX tp = ps[a];
        ps[a] = ps[b];
        ps[b] = tp;
        double td = ds[a];
        ds[a] = ds[b];
        ds[b] = td;
    }
}