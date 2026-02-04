package win.demistorm.visual_health.client;

import net.minecraft.world.entity.EntityType;
import win.demistorm.visual_health.VisualHealth;

import java.util.HashMap;
import java.util.Map;

// Manages entity-specific damage color and emissive overrides
// Hardcoded mappings for mobs with custom wound appearances
public class EntityDamageColors {

    // Custom damage settings for a specific entity type
    public record DamageOverride(int tintColor, boolean isEmissive) {
        // tintColor: ARGB hex color (e.g., 0xFFFF4010 for red)
        // isEmissive: whether to use emissive rendering (glow effect)
        //   - true: uses entityTranslucentEmissive render type with full brightness
        //   - false: uses entityCutoutNoCull with normal lighting
        //   Emissive rendering creates a true glow/bloom effect visible in darkness
    }

    // Hardcoded map of entity type overrides
    // Add new entries here to customize damage appearance for specific mobs
    private static final Map<EntityType<?>, DamageOverride> OVERRIDE_MAP = new HashMap<>();

    static {
        // Creeper: Dark green damage to match its explosive nature
        OVERRIDE_MAP.put(EntityType.CREEPER, new DamageOverride(0xFF204020, false));

        // Enderman: Light purple damage with emissive glow
        // Uses entityTranslucentEmissive for true glow effect in dark areas
        OVERRIDE_MAP.put(EntityType.ENDERMAN, new DamageOverride(0xFFD080FF, true));

        // Zombie: Sickly yellow-green damage for infected appearance
        OVERRIDE_MAP.put(EntityType.ZOMBIE, new DamageOverride(0xFF863B22, false));

        OVERRIDE_MAP.put(EntityType.SKELETON, new DamageOverride(0xFFA79F97, false));

        OVERRIDE_MAP.put(EntityType.STRAY, new DamageOverride(0xFFA79F97, false));

        OVERRIDE_MAP.put(EntityType.WITHER_SKELETON, new DamageOverride(0xFFA79F97, false));

        OVERRIDE_MAP.put(EntityType.WITHER, new DamageOverride(0xFFA79F97, false));

        OVERRIDE_MAP.put(EntityType.SPIDER, new DamageOverride(0xFFC4D3FF, false));

        OVERRIDE_MAP.put(EntityType.CAVE_SPIDER, new DamageOverride(0xFFC4D3FF, false));

        OVERRIDE_MAP.put(EntityType.HUSK, new DamageOverride(0xFF863B22, false));

        OVERRIDE_MAP.put(EntityType.ZOMBIE_HORSE, new DamageOverride(0xFF863B22, false));

        OVERRIDE_MAP.put(EntityType.ZOMBIE_NAUTILUS, new DamageOverride(0xFF863B22, false));

        OVERRIDE_MAP.put(EntityType.ZOMBIE_VILLAGER, new DamageOverride(0xFF863B22, false));


        VisualHealth.LOGGER.info("Loaded {} entity damage overrides", OVERRIDE_MAP.size());
    }

    // Get damage override for an entity type, or null if none exists
    public static DamageOverride getOverride(EntityType<?> entityType) {
        return OVERRIDE_MAP.get(entityType);
    }

    // Check if an entity type has a custom override
    public static boolean hasOverride(EntityType<?> entityType) {
        return OVERRIDE_MAP.containsKey(entityType);
    }
}
