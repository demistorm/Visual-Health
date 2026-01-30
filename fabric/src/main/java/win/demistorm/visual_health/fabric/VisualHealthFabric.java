package win.demistorm.visual_health.fabric;

import net.fabricmc.api.ModInitializer;
import win.demistorm.visual_health.VisualHealth;

public final class VisualHealthFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        // Run common setup
        VisualHealth.initialize();
    }
}
