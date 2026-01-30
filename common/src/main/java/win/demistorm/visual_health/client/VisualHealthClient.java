package win.demistorm.visual_health.client;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

// Client initialization (called by each platform)
public class VisualHealthClient {

    private static final Logger log = LogManager.getLogger(VisualHealthClient.class);

    // Set up clientside systems
    public static void initializeClient() {
        log.info("Visual Health (CLIENT) starting!");
    }
}
