package win.demistorm.visual_health.client;

import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.damagesource.DamageSource;
import win.demistorm.visual_health.VisualHealth;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class DamageEventHandler {

    private DamageEventHandler() {
    }

    private static final Map<Integer, DamageType> LAST_DAMAGE_TYPE = new ConcurrentHashMap<>();

    public static DamageType getLastDamageType(int entityId) {
        return LAST_DAMAGE_TYPE.getOrDefault(entityId, DamageType.SWORD);
    }

    public static void clearAllCaches() {
        int cacheSize = LAST_DAMAGE_TYPE.size();
        LAST_DAMAGE_TYPE.clear();

        if (cacheSize > 0) {
            VisualHealth.LOGGER.info("Cleared {} last damage type mappings on resource reload", cacheSize);
        }
    }

    public static void onLivingDamage(LivingEntity entity, DamageSource source) {
        if (entity == null) {
            return;
        }

        DamageType damageType = detectDamageType(source);

        int entityId = entity.getId();
        LAST_DAMAGE_TYPE.put(entityId, damageType);

        if (VisualHealth.debugMode) {
            int currentTier = EntityHealthTracker.getDamageTier(entityId);
            VisualHealth.LOGGER.debug("Entity {} (ID: {}) damaged by {}, current tier: {}",
                    entity.getName().getString(), entityId, damageType, currentTier);
        }
    }

    private static DamageType detectDamageType(DamageSource source) {
        if (source.getEntity() instanceof Player attacker) {
            ItemStack weapon = attacker.getMainHandItem();

            if (weapon.isEmpty()) {
                return DamageType.GENERIC;
            }

            if (weapon.is(ItemTags.SWORDS)) {
                return DamageType.SWORD;
            } else if (weapon.is(ItemTags.AXES)) {
                return DamageType.AXE;
            } else if (weapon.is(ItemTags.TRIDENT_ENCHANTABLE)) {
                return DamageType.TRIDENT;
            } else if (weapon.is(ItemTags.SPEARS)) {
                return DamageType.SPEAR; // Added in 1.21.11!
            }

            return DamageType.GENERIC;
        }

        return DamageType.GENERIC;
    }
}
