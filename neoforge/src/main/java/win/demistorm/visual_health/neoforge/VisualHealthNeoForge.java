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

        // Register clientside events
        if (FMLEnvironment.dist.isClient()) {
            NeoClient.register(modEventBus);
            win.demistorm.visual_health.neoforge.client.NeoForgeConfigScreen.register();
        }

        VisualHealth.LOGGER.info("Visual Health (NEOFORGE) initialization complete!");
    }
}
