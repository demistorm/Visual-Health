package win.demistorm.visual_health.client;

import net.minecraft.world.entity.EntityType;
import static net.minecraft.world.entity.EntityType.*;
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

        VisualHealth.LOGGER.info("Loaded {} entity damage overrides", OVERRIDE_MAP.size());
    }

    private static void OC(EntityType<?> entityType, String colorHex, boolean isEmissive) {
        int color = 0xFF000000 | Integer.parseInt(colorHex, 16);
        OVERRIDE_MAP.put(entityType, new DamageOverride(color, isEmissive));
    }

    public static DamageOverride getOverride(EntityType<?> entityType) {
        return OVERRIDE_MAP.get(entityType);
    }
}
