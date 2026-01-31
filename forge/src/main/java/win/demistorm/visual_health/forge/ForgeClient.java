package win.demistorm.visual_health.forge;

import net.minecraftforge.fml.common.Mod;
import win.demistorm.visual_health.VisualHealth;
import win.demistorm.visual_health.client.VisualHealthClient;

// Forge client initialization
public class ForgeClient {

    // Initialize client directly (no event system needed)
    public static void initialize() {
        VisualHealthClient.initializeClient();
        // Note: registerDamageLayers() will retry internally if renderers aren't ready yet
        VisualHealthClient.registerDamageLayers();
    }
}
