package win.demistorm.visual_health.client.entitymappings;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import static net.minecraft.world.entity.EntityTypes.*;
import win.demistorm.visual_health.VisualHealth;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public class EntityDamageColors {

    public record DamageOverride(int tintColor, boolean isEmissive) {
    }

    private static final Map<EntityType<?>, DamageOverride> OVERRIDE_MAP = new HashMap<>();
    private static final Map<EntityType<?>, DamageOverride> USER_OVERRIDE_MAP = new HashMap<>();
    private static final Set<EntityType<?>> DISABLED_OVERRIDE_SET = new HashSet<>();

    private static final Map<String, Integer> PRESET_COLORS = Map.of(
            "RED", 0xFF9F0000,
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
        OC(ZOMBIE_NAUTILUS, "863B22", false);
        OC(ZOMBIE_VILLAGER, "863B22", false);
        OC(SQUID, "2323c1", false);
        OC(GLOW_SQUID, "00cdcd", false);
        OC(STRIDER, "e6e600", false);
        OC(CAMEL_HUSK, "863B22", false);
        OC(NAUTILUS, "2323c1", false);
        OC(PUFFERFISH, "ffffb7", false);
        OC(ZOMBIE_NAUTILUS, "467f70", false);
        OC(ZOMBIFIED_PIGLIN, "863B22", false);
        OC(BOGGED, "776E65", false);
        OC(BREEZE, "c2c2c2", false);
        OC(CREAKING, "ff6b1a", false);
        OC(GUARDIAN, "2323c1", false);
        OC(ELDER_GUARDIAN, "2323c1", false);
        OC(MAGMA_CUBE, "8d2f00", false);
        OC(PARCHED, "c2c2c2", false);
        OC(SHULKER, "ae6984", false);
        OC(SLIME, "9db45f", false);
        OC(WARDEN, "008388", false);
        OC(ENDER_DRAGON, "D080FF", true);

        VisualHealth.LOGGER.info("Loaded {} entity damage overrides", OVERRIDE_MAP.size());
    }

    private static void OC(EntityType<?> entityType, String colorHex, boolean isEmissive) {
        int rgb = Integer.parseInt(colorHex, 16);
        int color = 0xFF000000 | rgb; // RGB hex layout matches ARGB
        OVERRIDE_MAP.put(entityType, new DamageOverride(color, isEmissive));
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
            Identifier rl = Identifier.tryParse(id);
            return BuiltInRegistries.ENTITY_TYPE.containsKey(rl);
        } catch (Exception e) {
            return false;
        }
    }

    public static String normalizeEntityId(String id) {
        return Objects.requireNonNull(Identifier.tryParse(id)).toString();
    }

    public static void applyUserOverrides(Map<String, String> overrides) {
        USER_OVERRIDE_MAP.clear();
        DISABLED_OVERRIDE_SET.clear();
        for (Map.Entry<String, String> entry : overrides.entrySet()) {
            EntityType<?> entityType;
            try {
                Optional<Holder.Reference<EntityType<?>>> holder = BuiltInRegistries.ENTITY_TYPE.get(Identifier.tryParse(entry.getKey()));
                if (holder.isEmpty()) {
                    VisualHealth.LOGGER.warn("Unknown entity type '{}' in color overrides, skipping", entry.getKey());
                    continue;
                }
                entityType = holder.get().value();
            } catch (Exception e) {
                VisualHealth.LOGGER.warn("Invalid entity type '{}' in color overrides, skipping", entry.getKey());
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
            int color = 0xFF000000 | rgb; // RGB hex layout matches ARGB
            return new DamageOverride(color, emissive);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
