package win.demistorm.visual_health.neoforge;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import win.demistorm.visual_health.VisualHealth;
import win.demistorm.visual_health.neoforge.client.NeoClient;

@Mod(VisualHealth.MOD_ID)
public final class VisualHealthNeoForge {

    public VisualHealthNeoForge(IEventBus modEventBus) {
        VisualHealth.LOGGER.info("Visual Health (NEOFORGE) starting!");

        VisualHealth.initialize();

        // Set up client side event handlers
        if (FMLEnvironment.getDist().isClient()) {
            NeoClient.register(modEventBus);
        }

        VisualHealth.LOGGER.info("Visual Health (NEOFORGE) initialization complete!");
    }
}
