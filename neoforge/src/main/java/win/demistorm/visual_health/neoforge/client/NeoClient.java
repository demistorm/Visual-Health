package win.demistorm.visual_health.neoforge.client;

import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;
import win.demistorm.visual_health.VisualHealth;
import win.demistorm.visual_health.client.VisualHealthClient;

// NeoForge client initialization
public class NeoClient {

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(NeoClient::onClientSetup);

        // Add reload listener registration
        modEventBus.addListener((AddClientReloadListenersEvent event) -> {
            VisualHealth.LOGGER.info("Registering Visual Health reload listener");

            Identifier listenerId = Identifier.fromNamespaceAndPath(
                    VisualHealth.MOD_ID,
                    "texture_reload"
            );

            event.addListener(listenerId, (sharedState,
                                           backgroundExecutor,
                                           preparationBarrier,
                                           gameExecutor) ->
                    preparationBarrier.wait(null).thenRunAsync(() -> {
                VisualHealth.LOGGER.info("Resource reload triggered - refreshing damage textures");
                VisualHealthClient.onResourcesReloaded();
            }, gameExecutor));
        });
    }

    // Register damage overlay layers during client setup
    private static void onClientSetup(FMLClientSetupEvent event) {
        VisualHealth.LOGGER.info("NeoForge client initialization starting");

        VisualHealthClient.initializeClient();
        VisualHealthClient.registerDamageLayers();

        VisualHealth.LOGGER.info("Hooked NeoForge reload listener registration");

        VisualHealth.LOGGER.info("NeoForge client initialization complete");
    }
}
