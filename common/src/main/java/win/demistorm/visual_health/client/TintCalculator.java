package win.demistorm.visual_health.client;

import net.minecraft.world.entity.LivingEntity;
import win.demistorm.visual_health.ConfigHelper;

// Calculates the appropriate tint color for wounds based on damage type and entity
// Uses hierarchical system: entity override > generic bruise color > config damage color
public final class TintCalculator {

    private TintCalculator() {
        // Utility class - no instances
    }

    // Brown bruise color for generic damage (punches, falls, etc.)
    private static final int GENERIC_BRUISE_COLOR = 0xFF8B4513; // Saddle brown

    // Get the appropriate tint color for a wound based on damage type and entity
    // Returns ARGB format color
    public static int getTintForDamageType(DamageType damageType, LivingEntity entity) {
        // Check for entity-specific damage color override first
        EntityDamageColors.DamageOverride override = EntityDamageColors.getOverride(entity.getType());
        if (override != null) {
            return override.tintColor();
        }

        // Generic damage (punches, falls, etc.) uses brown bruise color
        if (damageType == DamageType.GENERIC) {
            return GENERIC_BRUISE_COLOR;
        }

        // All weapon damage types (sword, axe, trident, spear) use config damage color
        return switch (ConfigHelper.INSTANCE.damageColor) {
            case RED -> 0xFF9F0000;   // Blood red
            case BLACK -> 0xFF000000; // Black
            case WHITE -> 0xFFFFFFFF; // Pure white
        };
    }
}
