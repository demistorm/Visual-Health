package win.demistorm.visual_health.client.entitymappings;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import static net.minecraft.world.entity.EntityType.*;
import win.demistorm.visual_health.VisualHealth;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


// Entity-specific damage color overrides
public class EntityDamageColors {

    public record DamageOverride(int tintColor, boolean isEmissive) {
    }

    // Override wound colors
    private static final Map<EntityType<?>, DamageOverride> OVERRIDE_MAP = new HashMap<>();

    // User overrides (always take priority)
    private static final Map<EntityType<?>, DamageOverride> USER_OVERRIDE_MAP = new HashMap<>();

    // User-disabled entities (no damage rendering at all)
    private static final Set<EntityType<?>> DISABLED_OVERRIDE_SET = new HashSet<>();

    private static final Map<String, Integer> PRESET_COLORS = Map.of(
            "RED", 0xFF00009F,
            "BLACK", 0xFF000000,
            "WHITE", 0xFFFFFFFF
    );

    static {
        OC(CREEPER, "204020", false);
        OC(ENDERMAN, "D080FF", true);
        OC(ZOMBIE, "863B22", false);
        OC(SKELETON, "776E65", false);
        OC(STRAY, "776E65", false);
        OC(WITHER_SKELETON, "776E65", false);
        OC(WITHER, "776E65", false);
        OC(SKELETON_HORSE, "776E65", false);
        OC(SPIDER, "C4D3FF", false);
        OC(CAVE_SPIDER, "C4D3FF", false);
        OC(HUSK, "863B22", false);
        OC(ZOMBIE_HORSE, "863B22", false);
        OC(ZOMBIE_VILLAGER, "863B22", false);
        OC(SQUID, "2323c1", false);
        OC(GLOW_SQUID, "00cdcd", false);
        OC(STRIDER, "e6e600", false);
        OC(PUFFERFISH, "ffffb7", false);
        OC(ZOMBIFIED_PIGLIN, "863B22", false);
        OC(GUARDIAN, "2323c1", false);
        OC(ELDER_GUARDIAN, "2323c1", false);
        OC(MAGMA_CUBE, "ddaf13", false);
        OC(SHULKER, "ae6984", false);
        OC(SLIME, "9db45f", false);
        OC(WARDEN, "008388", false);

        VisualHealth.LOGGER.info("Loaded {} entity damage overrides", OVERRIDE_MAP.size());
    }

    private static void OC(EntityType<?> entityType, String colorHex, boolean isEmissive) {
        int rgb = Integer.parseInt(colorHex, 16);
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        int abgr = 0xFF000000 | (b << 16) | (g << 8) | r;
        OVERRIDE_MAP.put(entityType, new DamageOverride(abgr, isEmissive));
    }

    public static boolean isDisabled(EntityType<?> entityType) {
        return DISABLED_OVERRIDE_SET.contains(entityType);
    }

    public static DamageOverride getOverride(EntityType<?> entityType) {
        DamageOverride user = USER_OVERRIDE_MAP.get(entityType);
        if (user != null) return user;
        return OVERRIDE_MAP.get(entityType);
    }

    public static boolean isValidEntityId(String id) {
        try {
            ResourceLocation rl = new ResourceLocation(id);
            return BuiltInRegistries.ENTITY_TYPE.containsKey(rl);
        } catch (Exception e) {
            return false;
        }
    }

    public static String normalizeEntityId(String id) {
        return new ResourceLocation(id).toString();
    }

    public static void applyUserOverrides(Map<String, String> overrides) {
        USER_OVERRIDE_MAP.clear();
        DISABLED_OVERRIDE_SET.clear();
        for (Map.Entry<String, String> entry : overrides.entrySet()) {
            EntityType<?> entityType;
            try {
                entityType = BuiltInRegistries.ENTITY_TYPE.get(new ResourceLocation(entry.getKey()));
            } catch (Exception e) {
                VisualHealth.LOGGER.warn("Invalid entity type '{}' in color overrides, skipping", entry.getKey());
                continue;
            }
            if (entityType == null) {
                VisualHealth.LOGGER.warn("Unknown entity type '{}' in color overrides, skipping", entry.getKey());
                continue;
            }
            String value = entry.getValue();
            if ("DISABLED".equals(value)) {
                DISABLED_OVERRIDE_SET.add(entityType);
                continue;
            }
            DamageOverride override = parseOverride(value);
            if (override != null) {
                USER_OVERRIDE_MAP.put(entityType, override);
            } else {
                VisualHealth.LOGGER.warn("Invalid color override '{}' for entity '{}', skipping", value, entry.getKey());
            }
        }
        VisualHealth.LOGGER.info("Applied {} color overrides, {} disabled entities", USER_OVERRIDE_MAP.size(), DISABLED_OVERRIDE_SET.size());
    }

    // Presets: "RED", "BLACK", "WHITE", "CUSTOM:FF0000", "EMISSIVE:FFFFFF"
    public static DamageOverride parseOverride(String value) {
        Integer preset = PRESET_COLORS.get(value);
        if (preset != null) {
            return new DamageOverride(preset, false);
        }
        if (value.startsWith("CUSTOM:")) {
            return parseHex(value.substring(7), false);
        }
        if (value.startsWith("EMISSIVE:")) {
            return parseHex(value.substring(9), true);
        }
        return null;
    }

    private static DamageOverride parseHex(String hex, boolean emissive) {
        if (hex.length() != 6) return null;
        try {
            int rgb = Integer.parseInt(hex, 16);
            int r = (rgb >> 16) & 0xFF;
            int g = (rgb >> 8) & 0xFF;
            int b = rgb & 0xFF;
            int abgr = 0xFF000000 | (b << 16) | (g << 8) | r;
            return new DamageOverride(abgr, emissive);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
