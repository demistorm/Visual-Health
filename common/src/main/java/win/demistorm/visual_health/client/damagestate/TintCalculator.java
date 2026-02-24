package win.demistorm.visual_health.client.damagestate;

import net.minecraft.world.entity.LivingEntity;
import win.demistorm.visual_health.ConfigHelper;
import win.demistorm.visual_health.client.entitymappings.DamageType;
import win.demistorm.visual_health.client.entitymappings.EntityDamageColors;

// Calculates the appropriate tint color for wounds based on damage type and entity
// Uses hierarchical system: generic bruise color > entity override for weapons > config weapon color
public final class TintCalculator {

    private TintCalculator() {
        // Utility class - no instances
    }

    // Brown bruise color for generic damage (punches, falls, etc.)
    private static final int GENERIC_BRUISE_COLOR = 0xFF8B4513; // Saddle brown

    // Get the appropriate tint color for a wound based on damage type and entity
    // Returns ARGB format color
    public static int getTintForDamageType(DamageType damageType, LivingEntity entity) {
        // Generic damage (punches, falls, etc.) ALWAYS uses brown bruise color
        // Entity overrides do NOT apply to generic damage
        if (damageType == DamageType.GENERIC) {
            return GENERIC_BRUISE_COLOR;
        }

        // For weapon damage (sword, axe, trident, spear), check entity-specific overrides first
        EntityDamageColors.DamageOverride override = EntityDamageColors.getOverride(entity.getType());
        if (override != null) {
            return override.tintColor();
        }

        // No override for weapon damage, use config damage color
        return switch (ConfigHelper.INSTANCE.damageColor) {
            case RED -> 0xFF9F0000;   // Blood red
            case BLACK -> 0xFF000000; // Black
            case WHITE -> 0xFFFFFFFF; // Pure white
        };
    }
}
