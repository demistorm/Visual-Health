package win.demistorm.visual_health.forge;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;
import win.demistorm.visual_health.VisualHealth;

// Forge mod entry point
@Mod(VisualHealth.MOD_ID)
public class VisualHealthForge {
    public VisualHealthForge() {
        VisualHealth.LOGGER.info("Visual Health (FORGE) starting!");

        // Run common setup
        VisualHealth.initialize();

        // Set up client side
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ForgeClient.initialize();
            win.demistorm.visual_health.forge.client.ForgeConfigScreen.register();
        }

        VisualHealth.LOGGER.info("Visual Health (FORGE) initialization complete!");
    }
}
