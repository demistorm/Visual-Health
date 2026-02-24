package win.demistorm.visual_health.neoforge.client;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import win.demistorm.visual_health.VisualHealth;
import win.demistorm.visual_health.client.SaveDamageCommand;
import win.demistorm.visual_health.client.VisualHealthClient;

// NeoForge client initialization
public class NeoClient {

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(NeoClient::onClientSetup);

        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent event) -> {
            SaveDamageCommand.register(event.getDispatcher());
        });

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

    private static void onClientSetup(FMLClientSetupEvent event) {
        VisualHealth.LOGGER.info("NeoForge client initialization starting");

        VisualHealthClient.initializeClient();
        VisualHealthClient.registerDamageLayers();

        VisualHealth.LOGGER.info("Hooked NeoForge reload listener registration");

        VisualHealth.LOGGER.info("NeoForge client initialization complete");
    }
}
