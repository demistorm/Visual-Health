package win.demistorm.visual_health.neoforge.client;

import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.fml.ModLoadingContext;
import win.demistorm.visual_health.client.config.ConfigScreen;

// NeoForge config screen registration
public class NeoForgeConfigScreen {

    public static void register() {
        ModLoadingContext.get().registerExtensionPoint(IConfigScreenFactory.class,
            () -> (minecraft, screen) -> ConfigScreen.VisualHealthConfigScreen.create(screen));
    }
}
