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
    public static final String VERSION = "1.7.10";

    private static final String[] FX_LAYERS_NAMES = new String[]{"field_78876_b", "fxLayers", "b"};

    private static boolean hasLoggedError = false;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        SPConfig.init(event.getSuggestedConfigurationFile());
        FMLCommonHandler.instance().bus().register(this);
    }

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

        // Guard compound operations on the particle lists.
        synchronized (client.effectRenderer) {

            if (limit == 0) {
                for (List<EntityFX> layer : fxLayers) {
                    if (!layer.isEmpty()) layer.clear();
                }
                return;
            }

            if (!smartCulling) {
                int total = 0;
                for (List<EntityFX> layer : fxLayers) {
                    total += layer.size();
                }
                if (total <= limit) return;
            }

            EntityClientPlayerMP player = client.thePlayer;

            Vec3 camPos = Vec3.createVectorHelper(
                player.prevPosX + (player.posX - player.prevPosX),
                player.prevPosY + (player.posY - player.prevPosY) + player.getEyeHeight(),
                player.prevPosZ + (player.posZ - player.prevPosZ)
            );

            Vec3 camDir = player.getLook(1.0F);

            int viewMode = client.gameSettings.thirdPersonView;
            if (viewMode > 0) {
                double dist = 4.0;
                if (viewMode == 2) {
                    camDir.xCoord = -camDir.xCoord;
                    camDir.yCoord = -camDir.yCoord;
                    camDir.zCoord = -camDir.zCoord;
                    camPos.xCoord -= camDir.xCoord * dist;
                    camPos.yCoord -= camDir.yCoord * dist;
                    camPos.zCoord -= camDir.zCoord * dist;
                } else {
                    camPos.xCoord -= camDir.xCoord * dist;
                    camPos.yCoord -= camDir.yCoord * dist;
                    camPos.zCoord -= camDir.zCoord * dist;
                }
            }

            double fov = client.gameSettings.fovSetting;
            double frustumThreshold = Math.cos(Math.toRadians((fov / 2.0) + 30.0));
            double frustumPenalty = 1.0e10;

            final double px = player.posX;
            final double py = player.posY;
            final double pz = player.posZ;

            final EntityFX[] heapParticles = new EntityFX[limit];
            final double[] heapScores = new double[limit];
            int heapSize = 0;

            // Snapshot each layer to avoid reading a live list that could change mid pass.
            for (List<EntityFX> layer : fxLayers) {
                EntityFX[] snapshot = layer.toArray(new EntityFX[0]);

                for (EntityFX p : snapshot) {
                    if (p == null) continue;

                    double pX = p.posX;
                    double pY = p.posY;
                    double pZ = p.posZ;

                    double dx = pX - px;
                    double dy = pY - py;
                    double dz = pZ - pz;
                    double distSq = dx * dx + dy * dy + dz * dz;

                    boolean inFrustum = false;

                    if (distSq <= 16.0) {
                        inFrustum = true;
                    } else {
                        double ex = pX - camPos.xCoord;
                        double ey = pY - camPos.yCoord;
                        double ez = pZ - camPos.zCoord;

                        double dot = ex * camDir.xCoord + ey * camDir.yCoord + ez * camDir.zCoord;

                        if (dot > 0) {
                            double eDistSq = ex * ex + ey * ey + ez * ez;
                            if (dot * dot > frustumThreshold * frustumThreshold * eDistSq) {
                                inFrustum = true;
                            }
                        }
                    }

                    if (smartCulling && !inFrustum) continue;

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

            Set<EntityFX> keep = Collections.newSetFromMap(new IdentityHashMap<EntityFX, Boolean>());
            for (int i = 0; i < heapSize; i++) {
                keep.add(heapParticles[i]);
            }

            // Remove from live lists while still holding the same lock.
            for (List<EntityFX> layer : fxLayers) {
                for (int i = layer.size() - 1; i >= 0; i--) {
                    EntityFX p = layer.get(i);
                    if (!keep.contains(p)) {
                        layer.remove(i);
                        p.setDead();
                    }
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
