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

// Client initialization (called by each platform)
public class VisualHealthClient {

    private static final Logger log = LogManager.getLogger(VisualHealthClient.class);
    private static boolean initialized = false;
    private static boolean layersRegistered = false;

    // Set up clientside systems
    public static void initializeClient() {
        if (initialized) {
            log.warn("Visual Health client already initialized, skipping");
            return;
        }

        log.info("Visual Health (CLIENT) starting!");

        // Load or create config file
        win.demistorm.visual_health.ConfigHelper.loadOrCreate();

        // Assets will be loaded lazily on first use (when entity takes damage)
        log.info("Visual Health wound textures will load on first use");

        initialized = true;
        log.info("Visual Health client initialization complete!");
    }

    // Force re-registration of damage overlay layers
    // Called on resource reload to ensure layers persist even if renderers are recreated
    public static void forceReRegisterLayers() {
        log.info("Forcing re-registration of damage overlay layers after resource reload");

        // Reset the flag to allow re-registration
        layersRegistered = false;

        // Re-register layers to all entity renderers
        boolean success = registerDamageLayers();

        if (success) {
            log.info("Successfully re-registered damage overlay layers after resource reload");
        } else {
            log.warn("Failed to re-register damage overlay layers after resource reload, will retry on next tick");
        }
    }

    // Called when resources are reloaded (F3+T, resource pack changes, etc.)
    // This method is loader-agnostic and should be called from loader-specific reload listeners
    public static void onResourcesReloaded() {
        log.info("Visual Health detected resource reload, performing full reset");

        // Step 1: Force re-registration of damage overlay layers
        // Must happen BEFORE cache clearing in case renderers were recreated
        forceReRegisterLayers();

        // Step 2: Clear all texture generation caches
        // These caches hold texture identifiers that become invalid after reload
        win.demistorm.visual_health.client.texture.EMFDamageTextureGenerator.clearAllCaches();
        win.demistorm.visual_health.client.texture.WoundTextureGenerator.clearAllCaches();

        // Step 3: Clear per-entity texture mappings (entity IDs can be reused after reload)
        win.demistorm.visual_health.client.EMFPerEntityTextures.clearAllCaches();

        // Step 4: Clear and reload wound texture identifiers
        WoundAssetSelector.cleanup();

        try {
            WoundAssetSelector.loadTextures();
            log.info("Visual Health reloaded wound textures successfully");
        } catch (Exception e) {
            log.error("Failed to reload wound textures", e);
        }

        // Send chat message to player
        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            client.player.displayClientMessage(
                    Component.literal("[Visual Health] Resource reload detected"),
                    true);
        }
        System.out.println("[Visual Health] Resource reload detected - full reset complete");
    }

    // Register damage overlay layers to all living entity renderers
    // Called after entity renderers are registered (in FMLClientSetupEvent for Forge/NeoForge, or onInitializeClient for Fabric)
    // Returns true if registration succeeded, false if it needs to be retried
    @SuppressWarnings({"rawtypes"})
    public static boolean registerDamageLayers() {
        if (layersRegistered) {
            if (VisualHealth.debugMode) {
                log.debug("Visual Health damage layers already registered, skipping");
            }
            return true;
        }

        log.info("Registering Visual Health damage overlay layers to entity renderers");

        Minecraft client = Minecraft.getInstance();

        int layersAdded = 0;
        int renderersProcessed = 0;

        // Access fields directly using access widener
        EntityRenderDispatcher dispatcher = client.getEntityRenderDispatcher();

        // Check if dispatcher exists yet (might be null during early initialization)
        //noinspection ConstantValue
        if (dispatcher == null) {
            log.warn("EntityRenderDispatcher is null, will retry later");
            return false;
        }

        // Check if renderers map is populated
        if (dispatcher.renderers.isEmpty()) {
            log.warn("Entity renderers map is empty, will retry later");
            return false;
        }

        log.debug("Found {} entity renderers in renderers map", dispatcher.renderers.size());

        // Add layer to each LivingEntityRenderer
        for (Object renderer : dispatcher.renderers.values()) {
            if (renderer instanceof LivingEntityRenderer livingRenderer) {
                addLayerToRenderer(livingRenderer);
                layersAdded++;
                if (VisualHealth.debugMode) {
                    log.debug("Added damage layer to renderer: {}",
                            livingRenderer.getClass().getSimpleName());
                }
            }
            renderersProcessed++;
        }

        // Handle player skin map
        log.debug("Found {} player renderers in playerRenderers map", dispatcher.playerRenderers.size());

        for (Object renderer : dispatcher.playerRenderers.values()) {
            if (renderer instanceof LivingEntityRenderer livingRenderer) {
                addLayerToRenderer(livingRenderer);
                layersAdded++;
                if (VisualHealth.debugMode) {
                    log.debug("Added damage layer to player renderer: {}",
                            livingRenderer.getClass().getSimpleName());
                }
            }
            renderersProcessed++;
        }

        layersRegistered = true;
        log.info("Visual Health registered {} damage overlay layers across {} renderers",
                layersAdded, renderersProcessed);
        return true;
    }

    // Helper method to add layer to a renderer (handles wildcard generics)
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void addLayerToRenderer(LivingEntityRenderer renderer) {
        // Access layers field directly using access widener
        // Add our damage overlay layer
        renderer.layers.add(new DamageOverlayLayer(renderer));
    }
}
