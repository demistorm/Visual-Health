package win.demistorm.visual_health.fabric.client;

import com.terraformersmc.modmenu.api.ModMenuApi;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import win.demistorm.visual_health.client.config.ConfigScreen;

// ModMenu integration for Fabric
// Allows config to be opened from ModMenu
public class FabricConfigScreen implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return ConfigScreen.VisualHealthConfigScreen::create;
    }
}
