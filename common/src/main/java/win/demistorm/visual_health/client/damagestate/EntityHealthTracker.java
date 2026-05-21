package win.demistorm.visual_health.client.damagestate;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import win.demistorm.visual_health.ConfigHelper;
import win.demistorm.visual_health.VisualHealth;
import win.demistorm.visual_health.client.entitymappings.DamageType;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class EntityHealthTracker {

    private EntityHealthTracker() {
    }

    private static final ThreadLocal<LivingEntity> CURRENT_RENDER_ENTITY = new ThreadLocal<>();
    private static final ThreadLocal<Identifier> CURRENT_RENDER_TEXTURE = new ThreadLocal<>();

    private static final Map<Integer, Integer> ENTITY_DAMAGE_TIERS = new ConcurrentHashMap<>();
    private static final Map<Integer, Map<Integer, DamageType>> ENTITY_TIER_DAMAGE_TYPES = new ConcurrentHashMap<>();
    private static final Map<Integer, Float> PREVIOUS_HEALTH = new ConcurrentHashMap<>();

    private static final Set<EntityType<?>> DISABLED_ENTITIES = Set.of(
            EntityType.IRON_GOLEM,
            EntityType.SNOW_GOLEM
    );

    public static void updateEntityDamageTier(LivingEntity entity) {
        if (entity == null) {
            return;
        }

        if (DISABLED_ENTITIES.contains(entity.getType())) {
            return;
        }

        int tier = calculateDamageTier(entity);
        int oldTier = ENTITY_DAMAGE_TIERS.getOrDefault(entity.getId(), 0);
        float currentHealth = entity.getHealth();
        float previousHealth = PREVIOUS_HEALTH.getOrDefault(entity.getId(), currentHealth);
        PREVIOUS_HEALTH.put(entity.getId(), currentHealth);

        if (tier > oldTier) {
            DamageType damageType = DamageEventHandler.getLastDamageType(entity.getId());
            if (damageType == DamageType.GENERIC) {
                float damage = previousHealth - currentHealth;
                if (damage > 3.0f) {
                    DamageEventHandler.setLastDamageType(entity.getId(), DamageType.SWORD);
                    damageType = DamageType.SWORD;
                    VisualHealth.LOGGER.debug("Damage fallback for entity {} (ID: {}): {} damage detected, switching GENERIC to SWORD",
                            entity.getName().getString(), entity.getId(), String.format("%.1f", damage));
                }
            }
            for (int t = oldTier + 1; t <= tier; t++) {
                setDamageTypeForTier(entity.getId(), t, damageType);
            }
        }

        ENTITY_DAMAGE_TIERS.put(entity.getId(), tier);

        if (tier < oldTier) {
            clearTiersAbove(entity.getId(), tier);
        }

        if (tier > 0 || oldTier != tier) {
            float healthPercent = (entity.getHealth() / entity.getMaxHealth()) * 100;
            VisualHealth.LOGGER.debug("Tracked damaged entity {} (ID: {}) -> Tier {} ({}% health)",
                    entity.getName().getString(), entity.getId(), tier, String.format("%.1f", healthPercent));
        }
    }

    public static int getDamageTier(int entityId) {
        return ENTITY_DAMAGE_TIERS.getOrDefault(entityId, 0);
    }

    private static int calculateDamageTier(LivingEntity entity) {
        float healthPercent = entity.getHealth() / entity.getMaxHealth();

        if (healthPercent >= 1.0f) return 0;

        int numTiers = ConfigHelper.INSTANCE.damageTierCount;
        float damagePercent = 1.0f - healthPercent;
        return Math.min(numTiers, (int)(damagePercent * numTiers) + 1);
    }

    public static void setDamageTypeForTier(int entityId, int tier, DamageType damageType) {
        ENTITY_TIER_DAMAGE_TYPES.computeIfAbsent(entityId, k -> new ConcurrentHashMap<>()).put(tier, damageType);
    }

    public static DamageType getDamageTypeForTier(int entityId, int tier) {
        Map<Integer, DamageType> tierMap = ENTITY_TIER_DAMAGE_TYPES.get(entityId);
        if (tierMap == null) {
            return DamageType.SWORD;
        }
        return tierMap.getOrDefault(tier, DamageType.SWORD);
    }

    private static void clearTiersAbove(int entityId, int maxTier) {
        Map<Integer, DamageType> tierMap = ENTITY_TIER_DAMAGE_TYPES.get(entityId);
        if (tierMap != null) {
            tierMap.keySet().removeIf(tier -> tier > maxTier);
        }

        win.demistorm.visual_health.client.texture.WoundTextureGenerator.clearEntityTiers(entityId, maxTier + 1,
                ConfigHelper.INSTANCE.damageTierCount);
    }

    public static void setCurrentRenderEntity(LivingEntity entity) {
        CURRENT_RENDER_ENTITY.set(entity);
    }

    public static LivingEntity getCurrentRenderEntity() {
        return CURRENT_RENDER_ENTITY.get();
    }

    public static void clearCurrentRenderEntity() {
        CURRENT_RENDER_ENTITY.remove();
    }

    public static void setCurrentRenderTexture(Identifier texture) {
        CURRENT_RENDER_TEXTURE.set(texture);
    }

    public static Identifier getCurrentRenderTexture() {
        return CURRENT_RENDER_TEXTURE.get();
    }

    public static void clearCurrentRenderTexture() {
        CURRENT_RENDER_TEXTURE.remove();
    }

    public static boolean hasWeaponTiers(int entityId, int damageTier) {
        for (int tier = 1; tier <= damageTier; tier++) {
            if (getDamageTypeForTier(entityId, tier) != DamageType.GENERIC) {
                return true;
            }
        }
        return false;
    }

    public static void clearAllCaches() {
        int tierCount = ENTITY_DAMAGE_TIERS.size();
        int damageTypeCount = ENTITY_TIER_DAMAGE_TYPES.size();

        ENTITY_DAMAGE_TIERS.clear();
        ENTITY_TIER_DAMAGE_TYPES.clear();
        PREVIOUS_HEALTH.clear();

        if (tierCount > 0 || damageTypeCount > 0) {
            VisualHealth.LOGGER.info("Cleared {} entity tier mappings and {} damage type mappings on resource reload",
                    tierCount, damageTypeCount);
        }
    }
}
