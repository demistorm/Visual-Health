package win.demistorm.visual_health.forge;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import win.demistorm.visual_health.VisualHealth;
import win.demistorm.visual_health.client.VisualHealthClient;

// Forge client initialization
@Mod.EventBusSubscriber(modid = VisualHealth.MOD_ID, value = Dist.CLIENT)
public class ForgeClient {

    // Initialize clientside platform code
    public static void initialize() {
        // Initialize clientside systems
        VisualHealthClient.initializeClient();
    }
}
