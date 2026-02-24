package win.demistorm.visual_health.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import win.demistorm.visual_health.VisualHealth;
import win.demistorm.visual_health.client.renderer.DamageOverlayLayer;
import win.demistorm.visual_health.client.renderer.WoundAssetSelector;

// Client initialization
public class VisualHealthClient {

    private static final Logger log = LogManager.getLogger(VisualHealthClient.class);
    private static boolean initialized = false;
    private static boolean layersRegistered = false;

    public static void initializeClient() {
        if (initialized) {
            log.warn("Visual Health client already initialized, skipping");
            return;
        }

        log.info("Visual Health (CLIENT) starting!");

        win.demistorm.visual_health.ConfigHelper.loadOrCreate();

        // Assets will be loaded on first use (when entity takes damage)
        log.info("Visual Health wound textures will load on first use");

        initialized = true;
        log.info("Visual Health client initialization complete!");
    }

    public static void forceReRegisterLayers() {
        log.info("Forcing re-registration of damage overlay layers after resource reload");

        layersRegistered = false;

        boolean success = registerDamageLayers();

        if (success) {
            log.info("Successfully re-registered damage overlay layers after resource reload");
        } else {
            log.warn("Failed to re-register damage overlay layers after resource reload, will retry on next tick");
        }
    }

    public static void onResourcesReloaded() {
        log.info("Visual Health detected resource reload, performing full reset");

        forceReRegisterLayers();

        win.demistorm.visual_health.client.texture.EMFDamageTextureGenerator.clearAllCaches();
        win.demistorm.visual_health.client.texture.WoundTextureGenerator.clearAllCaches();

        win.demistorm.visual_health.client.EMFPerEntityTextures.clearAllCaches();

        WoundAssetSelector.cleanup();

        try {
            WoundAssetSelector.loadTextures();
            log.info("Visual Health reloaded wound textures successfully");
        } catch (Exception e) {
            log.error("Failed to reload wound textures", e);
        }


        System.out.println("[Visual Health] Resource reload detected, full reset complete");
    }

    @SuppressWarnings({"rawtypes"})
    public static boolean registerDamageLayers() {
        if (layersRegistered) {
            return true;
        }

        log.info("Registering Visual Health damage overlay layers to entity renderers");

        Minecraft client = Minecraft.getInstance();

        int layersAdded = 0;
        int renderersProcessed = 0;

        EntityRenderDispatcher dispatcher = client.getEntityRenderDispatcher();

        if (dispatcher == null) {
            log.warn("EntityRenderDispatcher is null, will retry later");
            return false;
        }

        if (dispatcher.renderers.isEmpty()) {
            log.warn("Entity renderers map is empty, will retry later");
            return false;
        }

        log.debug("Found {} entity renderers in renderers map", dispatcher.renderers.size());

        for (Object renderer : dispatcher.renderers.values()) {
            if (renderer instanceof LivingEntityRenderer livingRenderer) {
                addLayerToRenderer(livingRenderer);
                layersAdded++;
            }
            renderersProcessed++;
        }

        log.debug("Found {} player renderers in playerRenderers map", dispatcher.playerRenderers.size());

        for (Object renderer : dispatcher.playerRenderers.values()) {
            if (renderer instanceof LivingEntityRenderer livingRenderer) {
                addLayerToRenderer(livingRenderer);
                layersAdded++;
            }
            renderersProcessed++;
        }

        layersRegistered = true;
        log.info("Visual Health registered {} damage overlay layers across {} renderers",
                layersAdded, renderersProcessed);
        return true;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void addLayerToRenderer(LivingEntityRenderer renderer) {
        renderer.layers.add(new DamageOverlayLayer(renderer));
    }
}
