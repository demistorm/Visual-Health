package win.demistorm.visual_health.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import win.demistorm.visual_health.VisualHealth;
import win.demistorm.visual_health.client.renderer.DamageOverlayLayer;
import win.demistorm.visual_health.client.renderer.WoundAssetSelector;

import java.util.Map;

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

        // Assets will be loaded lazily on first use (when entity takes damage)
        log.info("Visual Health wound textures will load on first use");

        // Register resource reload listener to handle resource pack changes
        // Cast to ReloadableResourceManager to access registerReloadListener
        Minecraft client = Minecraft.getInstance();
        if (client != null && client.getResourceManager() instanceof ReloadableResourceManager) {
            ReloadableResourceManager reloadManager = (ReloadableResourceManager) client.getResourceManager();
            reloadManager.registerReloadListener(new ResourceManagerReloadListener() {
                @Override
                public void onResourceManagerReload(ResourceManager resourceManager) {
                    log.info("Visual Health detected resource reload, clearing wound texture cache");

                    // Clear wound texture identifiers
                    WoundAssetSelector.cleanup();

                    // Reload wound texture identifiers
                    try {
                        WoundAssetSelector.loadTextures();
                        log.info("Visual Health reloaded wound textures successfully");
                    } catch (Exception e) {
                        log.error("Failed to reload wound textures", e);
                    }
                }
            });
        }

        initialized = true;
        log.info("Visual Health client initialization complete!");
    }

    // Register damage overlay layers to all living entity renderers
    // Called after entity renderers are registered (in FMLClientSetupEvent for Forge/NeoForge, or onInitializeClient for Fabric)
    // Returns true if registration succeeded, false if it needs to be retried
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static boolean registerDamageLayers() {
        if (layersRegistered) {
            if (VisualHealth.debugMode) {
                log.debug("Visual Health damage layers already registered, skipping");
            }
            return true;
        }

        log.info("Registering Visual Health damage overlay layers to entity renderers");

        Minecraft client = Minecraft.getInstance();
        if (client == null || client.getEntityRenderDispatcher() == null) {
            log.warn("EntityRenderDispatcher not available yet, will retry later");
            return false;
        }

        int layersAdded = 0;
        int renderersProcessed = 0;

        // Access fields directly using access widener
        EntityRenderDispatcher dispatcher = client.getEntityRenderDispatcher();

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
