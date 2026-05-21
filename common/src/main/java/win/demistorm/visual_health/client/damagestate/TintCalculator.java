package win.demistorm.visual_health.client.damagestate;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import win.demistorm.visual_health.ConfigHelper;
import win.demistorm.visual_health.client.entitymappings.DamageType;
import win.demistorm.visual_health.client.entitymappings.EntityDamageColors;
import win.demistorm.visual_health.client.texture.SkinColorSampler;
import win.demistorm.visual_health.client.texture.TintUtils;

public final class TintCalculator {

    private TintCalculator() {
    }

    public static final int BRUISE_BROWN = 0xFF503F36;
    public static final float BRUISE_BLEND_RATIO = 0.35f;
    public static final int FALLBACK_BRUISE_COLOR = 0xFF503F36;

    public static int getTintForDamageType(DamageType damageType, LivingEntity entity) {
        if (damageType == DamageType.GENERIC) {
            return TintUtils.blendColors(getWeaponTint(entity), BRUISE_BROWN, BRUISE_BLEND_RATIO);
        }

        return getWeaponTint(entity);
    }

    public static int getWeaponTint(LivingEntity entity) {
        EntityDamageColors.DamageOverride override = EntityDamageColors.getOverride(entity.getType());
        if (override != null) {
            return override.tintColor();
        }

        if (entity instanceof Player && ConfigHelper.INSTANCE.playerSampledDamage) {
            return SkinColorSampler.getSampledTint(entity);
        }

        return switch (ConfigHelper.INSTANCE.damageColor) {
            case RED -> 0xFF9F0000;
            case BLACK -> 0xFF000000;
            case WHITE -> 0xFFFFFFFF;
        };
    }
}
