package win.demistorm.visual_health.fabric.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import win.demistorm.visual_health.client.VisualHealthClient;
import win.demistorm.visual_health.VisualHealth;

public final class FabricClient implements ClientModInitializer {

    private static boolean registrationSuccessful = false;

    @Override
    public void onInitializeClient() {
        VisualHealth.LOGGER.info("Fabric client initialization starting");

        // Initialize clientside systems
        VisualHealthClient.initializeClient();

        // Try to register damage overlay layers immediately
        registrationSuccessful = VisualHealthClient.registerDamageLayers();

        // Set up end-of-tick callback to retry registration if needed
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!registrationSuccessful) {
                VisualHealth.LOGGER.debug("Retrying damage layer registration...");
                registrationSuccessful = VisualHealthClient.registerDamageLayers();
            }
        });

        // Note: Damage event tracking is now handled via LivingEntityMixin
        // This works on both Fabric and NeoForge without platform-specific code

        VisualHealth.LOGGER.info("Fabric client initialization complete");
    }
}
