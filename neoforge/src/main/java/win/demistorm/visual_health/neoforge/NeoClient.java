package win.demistorm.visual_health.neoforge;

import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import net.minecraft.server.packs.resources.ResourceManager;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.IModBusEvent;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;
import win.demistorm.visual_health.VisualHealth;
import win.demistorm.visual_health.client.VisualHealthClient;
import win.demistorm.visual_health.neoforge.client.VisualHealthNeoForgeReloadListener;

// NeoForge client initialization
public class NeoClient {

    // Register NeoForge event handler
    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(NeoClient::onClientSetup);
    }

    // Register damage overlay layers during client setup
    private static void onClientSetup(FMLClientSetupEvent event) {
        VisualHealth.LOGGER.info("NeoForge client initialization starting");

        VisualHealthClient.initializeClient();
        VisualHealthClient.registerDamageLayers();

        VisualHealthNeoForgeReloadListener.onResourceManagerReload();

        VisualHealth.LOGGER.info("Hooked NeoForge reload listener registration");

        VisualHealth.LOGGER.info("NeoForge client initialization complete");
    }
}
