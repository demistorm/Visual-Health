package win.demistorm.visual_health.forge.client;

import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.ModLoadingContext;
import win.demistorm.visual_health.client.config.ConfigScreen;

// Forge config screen registration
public class ForgeConfigScreen {
    public static void register() {
        ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
            () -> new ConfigScreenHandler.ConfigScreenFactory((mc, screen) -> ConfigScreen.VisualHealthConfigScreen.create(screen)));
    }
}
