package win.demistorm.visual_health.client;

import net.minecraft.world.entity.EntityType;
import win.demistorm.visual_health.VisualHealth;

import java.util.HashMap;
import java.util.Map;

// Manages entity-specific damage color and emissive overrides
// Hardcoded mappings for mobs with custom wound appearances
public class EntityDamageOverrides {

    // Custom damage settings for a specific entity type
    public record DamageOverride(int tintColor, boolean isEmissive) {
        // tintColor: ARGB hex color (e.g., 0xFFFF4010 for red)
        // isEmissive: whether to render at full brightness (glow in dark)
    }

    // Hardcoded map of entity type overrides
    // Add new entries here to customize damage appearance for specific mobs
    private static final Map<EntityType<?>, DamageOverride> OVERRIDE_MAP = new HashMap<>();

    static {
        // Creeper: Dark green damage to match its explosive nature
        OVERRIDE_MAP.put(EntityType.CREEPER, new DamageOverride(0xFF204020, false));

        // Enderman: Light purple damage that glows (emissive) for otherworldly effect
        OVERRIDE_MAP.put(EntityType.ENDERMAN, new DamageOverride(0xFFB080FF, true));

        // Zombie: Sickly yellow-green damage for infected appearance
        OVERRIDE_MAP.put(EntityType.ZOMBIE, new DamageOverride(0xFFA0A030, false));

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
