package win.demistorm.visual_health.fabric.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import win.demistorm.visual_health.client.DamageEventHandler;
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

        // Register damage event handler for weapon type tracking
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            // Track damage on client side for visual wound system
            if (entity.level().isClientSide()) {
                DamageEventHandler.onLivingDamage(entity, source);
            }
            // Return true to allow damage (we're just tracking, not cancelling)
            return true;
        });

        VisualHealth.LOGGER.info("Fabric client initialization complete (damage events registered)");
    }
}
