package win.demistorm.visual_health.forge;

import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import win.demistorm.visual_health.VisualHealth;
import win.demistorm.visual_health.client.VisualHealthClient;

// Forge client initialization
@Mod.EventBusSubscriber(modid = VisualHealth.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ForgeClient {

    // Register reload listener during mod construction
    @SubscribeEvent
    public static void onRegisterReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(
                new net.minecraft.server.packs.resources.ResourceManagerReloadListener() {
                    @Override
                    public void onResourceManagerReload(ResourceManager resourceManager) {
                        VisualHealthClient.onResourcesReloaded();
                    }
                }
        );
    }

    // Initialize client systems
    public static void initialize() {
        VisualHealth.LOGGER.info("Forge client initialization starting");

        VisualHealthClient.initializeClient();
        // Note: registerDamageLayers() will retry internally if renderers aren't ready yet
        VisualHealthClient.registerDamageLayers();

        // TODO: Register Forge damage event handler for weapon type tracking
        // Requires subscribing to Forge event bus on client side
        // For now, weapon detection defaults to GENERIC type
        // The mod works without this - wounds still appear with generic textures

        VisualHealth.LOGGER.info("Forge client initialization complete");
    }
}
