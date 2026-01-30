package win.demistorm.visual_health;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;

public final class VisualHealth {
    public static final String MOD_ID = "visual_health";
    public static final Logger LOGGER = LogManager.getLogger(VisualHealth.class);

    // Debug mode switch
    public static final boolean debugMode = false;

    static {
        Configurator.setLevel(MOD_ID, debugMode ? Level.DEBUG : Level.INFO);
    }

    public static void initialize() {
        // Initialize mod
    }
}
