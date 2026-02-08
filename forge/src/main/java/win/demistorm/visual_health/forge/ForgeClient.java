package win.demistorm.visual_health.forge;

import net.minecraftforge.fml.common.Mod;
import win.demistorm.visual_health.VisualHealth;
import win.demistorm.visual_health.client.VisualHealthClient;

// Forge client initialization
public class ForgeClient {

    // Initialize client directly (no event system needed)
    public static void initialize() {
        VisualHealth.LOGGER.info("Forge client initialization starting");

        VisualHealthClient.initializeClient();
        // Note: registerDamageLayers() will retry internally if renderers aren't ready yet
        VisualHealthClient.registerDamageLayers();

        // TODO: Register Forge damage event handler for weapon type tracking
        // Requires subscribing to Forge event bus on client side
        // For now, weapon detection defaults to GENERIC type
        // The mod works without this - wounds still appear with generic textures

        VisualHealth.LOGGER.info("Forge client initialization complete");
    }
}
