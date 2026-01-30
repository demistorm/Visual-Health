package win.demistorm.visual_health.neoforge;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import win.demistorm.visual_health.VisualHealth;

@Mod(VisualHealth.MOD_ID)
public final class VisualHealthNeoForge {

    public VisualHealthNeoForge(IEventBus modEventBus) {
        VisualHealth.LOGGER.info("Visual Health (NEOFORGE) starting!");

        VisualHealth.initialize();

        // Set up client side
        if (FMLEnvironment.getDist().isClient()) {
            NeoClient.initialize();
        }

        VisualHealth.LOGGER.info("Visual Health (NEOFORGE) initialization complete!");
    }
}
