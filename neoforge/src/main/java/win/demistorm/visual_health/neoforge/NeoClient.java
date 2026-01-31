package win.demistorm.visual_health.neoforge;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import win.demistorm.visual_health.VisualHealth;
import win.demistorm.visual_health.client.VisualHealthClient;

// NeoForge client initialization
public class NeoClient {

    // Register NeoForge event handler
    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(NeoClient::onClientSetup);
    }

    // Register damage overlay layers during client setup
    private static void onClientSetup(FMLClientSetupEvent event) {
        VisualHealthClient.initializeClient();
        VisualHealthClient.registerDamageLayers();
    }
}
