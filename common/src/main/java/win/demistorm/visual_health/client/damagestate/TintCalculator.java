package win.demistorm.visual_health.client.damagestate;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import win.demistorm.visual_health.ConfigHelper;
import win.demistorm.visual_health.client.entitymappings.DamageType;
import win.demistorm.visual_health.client.entitymappings.EntityDamageColors;
import win.demistorm.visual_health.client.texture.SkinColorSampler;
import win.demistorm.visual_health.client.texture.TintUtils;

// Calculates the appropriate tint color for wounds based on damage type and entity
// Generic wounds blend the entity's weapon tint with brown for bruise look
// Weapon wounds use entity overrides, skin sampling, or config color
public final class TintCalculator {

    private TintCalculator() {
        // Utility class - no instances
    }

    // Dark brown used to blend with wound color
    // ABGR format: A=FF, B=36, G=3F, R=50
    public static final int BRUISE_BROWN = 0xFF363F50;

    public static final float BRUISE_BLEND_RATIO = 0.35f;
    public static final int FALLBACK_BRUISE_COLOR = 0xFF363F50;

    // Get the appropriate tint color for a wound based on damage type and entity
    // Returns ABGR format color
    public static int getTintForDamageType(DamageType damageType, LivingEntity entity) {
        if (damageType == DamageType.GENERIC) {
            return TintUtils.blendColors(getWeaponTint(entity), BRUISE_BROWN, BRUISE_BLEND_RATIO);
        }

        return getWeaponTint(entity);
    }

    // Check the weapon tint for an entity (entity override > skin sample > config)
    public static int getWeaponTint(LivingEntity entity) {
        EntityDamageColors.DamageOverride override = EntityDamageColors.getOverride(entity.getType());
        if (override != null) {
            return override.tintColor();
        }

        // No override for weapon damage, use config damage color (ABGR format)
        if (entity instanceof Player && ConfigHelper.INSTANCE.playerSampledDamage) {
            return SkinColorSampler.getSampledTint(entity);
        }

        return switch (ConfigHelper.INSTANCE.damageColor) {
            case RED -> 0xFF00009F;   // Blood red (A=FF, B=00, G=00, R=9F)
            case BLACK -> 0xFF000000; // Black
            case WHITE -> 0xFFFFFFFF; // Pure white
        };
    }
}
