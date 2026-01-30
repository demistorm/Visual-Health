package win.demistorm.visual_health.client;

import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import win.demistorm.visual_health.VisualHealth;
import win.demistorm.visual_health.client.texture.TextureDamageManager;
import win.demistorm.visual_health.client.texture.WoundAssetLoader;

// Client initialization (called by each platform)
public class VisualHealthClient {

    private static final Logger log = LogManager.getLogger(VisualHealthClient.class);
    private static boolean initialized = false;

    // Set up clientside systems
    public static void initializeClient() {
        if (initialized) {
            log.warn("Visual Health client already initialized, skipping");
            return;
        }

        log.info("Visual Health (CLIENT) starting!");

        // Assets will be loaded lazily on first use (when entity takes damage)
        log.info("Visual Health wound assets will load on first use");

        // Register resource reload listener to handle resource pack changes
        // Cast to ReloadableResourceManager to access registerReloadListener
        Minecraft client = Minecraft.getInstance();
        if (client != null && client.getResourceManager() instanceof ReloadableResourceManager) {
            ReloadableResourceManager reloadManager = (ReloadableResourceManager) client.getResourceManager();
            reloadManager.registerReloadListener(new ResourceManagerReloadListener() {
                @Override
                public void onResourceManagerReload(ResourceManager resourceManager) {
                    log.info("Visual Health detected resource reload, clearing caches");

                    // Clear texture damage cache
                    TextureDamageManager.clearCache();

                    // Reload wound assets
                    try {
                        WoundAssetLoader.cleanup();
                        WoundAssetLoader.loadAssets();
                        log.info("Visual Health reloaded wound assets successfully");
                    } catch (Exception e) {
                        log.error("Failed to reload wound assets", e);
                    }
                }
            });
        }

        initialized = true;
        log.info("Visual Health client initialization complete!");
    }
}
