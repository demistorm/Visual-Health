package win.demistorm.visual_health;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

// Client-side config for Visual Health
public final class ConfigHelper {

    public enum DamageColor {
        RED,
        BLACK,
        WHITE
    }

    public static final class Data {
        public int woundDensityPercentage = 50;

        public boolean damagePassiveMobs = true;
        public boolean damageVillagers = false;
        public boolean damagePlayers = true;
        public boolean drawOnOptifineEmissives = false;
        public DamageColor damageColor = DamageColor.RED;

        // Entity-specific overrides
        public Map<String, String> colorOverrides = new LinkedHashMap<>();
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_DIR = Path.of("config");
    private static final Path CONFIG_FILE = CONFIG_DIR.resolve("visualhealth.json");

    public static final Data INSTANCE = new Data();

    private static String toJson(Data d) {
        return GSON.toJson(d);
    }

    private static Data fromJson(String js) {
        return GSON.fromJson(js, Data.class);
    }

    public static void loadOrCreate() {
        Data loaded = read();
        write(loaded);
        copyInto(loaded);
        win.demistorm.visual_health.client.entitymappings.EntityDamageColors.applyUserOverrides(INSTANCE.colorOverrides);
        VisualHealth.LOGGER.info("Visual Health config loaded: {}% wound density, passive mobs: {}, villagers: {}, players: {}, color: {}",
                INSTANCE.woundDensityPercentage, INSTANCE.damagePassiveMobs, INSTANCE.damageVillagers, INSTANCE.damagePlayers, INSTANCE.damageColor);
    }

    private static Data read() {
        try {
            if (Files.exists(CONFIG_FILE)) {
                return fromJson(Files.readString(CONFIG_FILE));
            }
        } catch (IOException e) {
            VisualHealth.LOGGER.error("Failed to read Visual Health config, using defaults", e);
        }
        return new Data();
    }

    public static void write(Data d) {
        try {
            Files.createDirectories(CONFIG_DIR);
            Files.writeString(CONFIG_FILE, toJson(d));
            VisualHealth.LOGGER.debug("Visual Health config saved");
        } catch (IOException e) {
            VisualHealth.LOGGER.error("Failed to write Visual Health config", e);
        }
    }

    private static void copyInto(Data from) {
        ConfigHelper.INSTANCE.woundDensityPercentage = from.woundDensityPercentage;
        ConfigHelper.INSTANCE.damagePassiveMobs = from.damagePassiveMobs;
        ConfigHelper.INSTANCE.damageVillagers = from.damageVillagers;
        ConfigHelper.INSTANCE.damagePlayers = from.damagePlayers;
        ConfigHelper.INSTANCE.drawOnOptifineEmissives = from.drawOnOptifineEmissives;
        ConfigHelper.INSTANCE.damageColor = from.damageColor;
        ConfigHelper.INSTANCE.colorOverrides = new LinkedHashMap<>(from.colorOverrides);
    }

    public static void save() {
        write(INSTANCE);
    }

    public static void clearTextureCaches() {
        win.demistorm.visual_health.client.texture.WoundTextureGenerator.clearTextureCaches();
        win.demistorm.visual_health.client.renderer.WoundAssetSelector.cleanup();
        VisualHealth.LOGGER.info("Visual Health texture caches cleared on config change (damage history preserved)");
    }
}
