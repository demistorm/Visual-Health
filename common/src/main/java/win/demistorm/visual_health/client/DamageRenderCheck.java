package win.demistorm.visual_health.client;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import win.demistorm.visual_health.ConfigHelper;
import win.demistorm.visual_health.client.entitymappings.EntityDamageColors;

import java.util.EnumSet;

public final class DamageRenderCheck {

    public enum DamageCheck {
        OVERRIDES,
        CONFIG,
        INVISIBLE
    }

    public static final DamageCheck[] ALL = DamageCheck.values();

    private DamageRenderCheck() {}

    public static boolean shouldRender(LivingEntity entity, DamageCheck... checks) {
        EnumSet<DamageCheck> set = checks.length == 0
                ? EnumSet.noneOf(DamageCheck.class)
                : EnumSet.of(checks[0], checks);

        if (set.contains(DamageCheck.OVERRIDES) && EntityDamageColors.isDisabled(entity.getType())) {
            return false;
        }

        if (set.contains(DamageCheck.CONFIG)) {
            if (entity instanceof Player) return ConfigHelper.INSTANCE.damagePlayers;
            if (ConfigHelper.INSTANCE.playerDamageOnly) return false;
            MobCategory category = entity.getType().getCategory();
            boolean isMonster = category == MobCategory.MONSTER;
            if (!isMonster && !ConfigHelper.INSTANCE.damagePassiveMobs) return false;
            if (entity instanceof AbstractVillager && !ConfigHelper.INSTANCE.damageVillagers) return false;
        }

        if (set.contains(DamageCheck.INVISIBLE) && entity.isInvisible()) {
            return false;
        }

        return true;
    }

    public static boolean shouldRenderCorpseDamage() {
        if (!ConfigHelper.INSTANCE.damagePlayers) return false;
        return !EntityDamageColors.isDisabled(EntityType.PLAYER);
    }
}
