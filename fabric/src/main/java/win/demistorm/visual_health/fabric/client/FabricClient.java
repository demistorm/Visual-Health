package win.demistorm.visual_health.fabric.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.server.packs.PackType;
import win.demistorm.visual_health.client.SaveDamageCommand;
import win.demistorm.visual_health.client.VisualHealthClient;
import win.demistorm.visual_health.VisualHealth;

public final class FabricClient implements ClientModInitializer {

    private static boolean registrationSuccessful = false;

    @Override
    public void onInitializeClient() {
        VisualHealth.LOGGER.info("Fabric client initialization starting");

        VisualHealthClient.initializeClient();

        registrationSuccessful = VisualHealthClient.registerDamageLayers();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!registrationSuccessful) {
                VisualHealth.LOGGER.debug("Retrying damage layer registration...");
                registrationSuccessful = VisualHealthClient.registerDamageLayers();
            }
        });

        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES)
                .registerReloadListener(new VisualHealthFabricReloadListener());

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            SaveDamageCommand.register(dispatcher);
        });

        VisualHealth.LOGGER.info("Fabric client initialization complete");
    }
}
