package win.demistorm.visual_health;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

// Handles client-side config file for visual health settings
// Simple config system since this mod is client-side only (no server sync needed)
public final class ConfigHelper {

    // Damage color options
    public enum DamageColor {
        RED,
        BLACK,
        WHITE
    }

    // Config settings with defaults
    public static final class Data {
        // Percentage-based wound density (10-100%)
        // Higher values = more wounds per tier, scales with texture size
        // 10% = sparse, 50% = medium density, 100% = maximum coverage
        public int woundDensityPercentage = 50;

        public boolean damagePassiveMobs = true;     // Show damage on passive mobs (animals, etc.)
        public boolean damageVillagers = false;      // Show damage on Villagers and Wandering Traders
        public DamageColor damageColor = DamageColor.RED; // Color of damage effects
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_DIR = Path.of("config");
    private static final Path CONFIG_FILE = CONFIG_DIR.resolve("visualhealth.json");

    // Single config instance (client-side only, no need for CLIENT/ACTIVE split)
    public static final Data INSTANCE = new Data();

    // Convert config to/from json
    private static String toJson(Data d) {
        return GSON.toJson(d);
    }

    private static Data fromJson(String js) {
        return GSON.fromJson(js, Data.class);
    }

    // Load or create config file with defaults
    public static void loadOrCreate() {
        Data loaded = read();
        write(loaded); // Ensure file exists with current values
        copyInto(loaded, INSTANCE);
        VisualHealth.LOGGER.info("Visual Health config loaded: {}% wound density, passive mobs: {}, villagers: {}, color: {}",
                INSTANCE.woundDensityPercentage, INSTANCE.damagePassiveMobs, INSTANCE.damageVillagers, INSTANCE.damageColor);
    }

    // Read config from disk
    private static Data read() {
        try {
            if (Files.exists(CONFIG_FILE)) {
                return fromJson(Files.readString(CONFIG_FILE));
            }
        } catch (IOException e) {
            VisualHealth.LOGGER.error("Failed to read Visual Health config, using defaults", e);
        }
        return new Data(); // Return defaults if file doesn't exist or error occurs
    }

    // Write config to disk
    public static void write(Data d) {
        try {
            Files.createDirectories(CONFIG_DIR);
            Files.writeString(CONFIG_FILE, toJson(d));
            VisualHealth.LOGGER.debug("Visual Health config saved");
        } catch (IOException e) {
            VisualHealth.LOGGER.error("Failed to write Visual Health config", e);
        }
    }

    // Copy values from one Data object to another
    private static void copyInto(Data from, Data to) {
        to.woundDensityPercentage = from.woundDensityPercentage;
        to.damagePassiveMobs = from.damagePassiveMobs;
        to.damageVillagers = from.damageVillagers;
        to.damageColor = from.damageColor;
    }

    // Save current INSTANCE to disk (convenience method for config screen)
    public static void save() {
        write(INSTANCE);
    }
}
