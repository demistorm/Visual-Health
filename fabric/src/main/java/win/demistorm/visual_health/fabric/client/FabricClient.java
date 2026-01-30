package win.demistorm.visual_health.fabric.client;

import net.fabricmc.api.ClientModInitializer;
import win.demistorm.visual_health.client.VisualHealthClient;

public final class FabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        // Initialize clientside systems
        VisualHealthClient.initializeClient();
    }
}
