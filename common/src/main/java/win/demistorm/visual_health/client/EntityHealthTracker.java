package win.demistorm.visual_health.client;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import win.demistorm.visual_health.VisualHealth;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

// Track damage tiers and damage types for entities by their ID
// In 1.21.11+, render state doesn't contain health, so we track it separately
public final class EntityHealthTracker {

    private EntityHealthTracker() {
        // Utility class - no instances
    }

    // Map entity ID -> damage tier (0-5)
    private static final Map<Integer, Integer> ENTITY_DAMAGE_TIERS = new ConcurrentHashMap<>();

    // Map entity ID -> (tier -> damage type)
    // Tracks which weapon caused damage at each tier
    private static final Map<Integer, Map<Integer, DamageType>> ENTITY_TIER_DAMAGE_TYPES = new ConcurrentHashMap<>();

    // Entities that should never show damage (hardcoded blacklist)
    private static final Set<EntityType<?>> DISABLED_ENTITIES = Set.of(
            EntityType.IRON_GOLEM,
            EntityType.COPPER_GOLEM,
            EntityType.SNOW_GOLEM
    );

    // Update the damage tier for an entity
    // Called during extractRenderState when entity data is fresh
    public static void updateEntityDamageTier(LivingEntity entity) {
        if (entity == null) {
            return;
        }

        // Skip damage tracking for disabled entities
        if (DISABLED_ENTITIES.contains(entity.getType())) {
            return;
        }

        int tier = calculateDamageTier(entity);
        int oldTier = ENTITY_DAMAGE_TIERS.getOrDefault(entity.getId(), 0);
        ENTITY_DAMAGE_TIERS.put(entity.getId(), tier);

        // If entity healed (tier decreased), clear texture cache and remove damage types for lost tiers
        if (tier < oldTier) {
            clearTiersAbove(entity.getId(), tier);
            VisualHealth.LOGGER.debug("Entity {} healed from tier {} to tier {}, cleared cache for lost tiers",
                    entity.getName().getString(), oldTier, tier);
        }

        // If entity took damage (tier increased), record the damage type for the new tier
        if (tier > oldTier) {
            // Get the last damage type from the DamageEventHandler
            DamageType damageType = DamageEventHandler.getLastDamageType(entity.getId());
            setDamageTypeForTier(entity.getId(), tier, damageType);

            if (VisualHealth.debugMode) {
                VisualHealth.LOGGER.debug("Entity {} tier increased {} -> {}, assigned damage type: {}",
                        entity.getName().getString(), oldTier, tier, damageType);
            }
        }

        // Only log if entity has damage (tier > 0) or tier changed
        if (tier > 0 || oldTier != tier) {
            float healthPercent = (entity.getHealth() / entity.getMaxHealth()) * 100;
            VisualHealth.LOGGER.info("Tracked damaged entity {} (ID: {}) -> Tier {} ({}% health)",
                    entity.getName().getString(), entity.getId(), tier, String.format("%.1f", healthPercent));

            if (VisualHealth.debugMode) {
                VisualHealth.LOGGER.debug("Entity {} health: {}/{}",
                        entity.getName().getString(), entity.getHealth(), entity.getMaxHealth());
            }
        }
    }

    // Get damage tier for an entity by its ID
    public static int getDamageTier(int entityId) {
        int tier = ENTITY_DAMAGE_TIERS.getOrDefault(entityId, 0);

        if (VisualHealth.debugMode) {
            VisualHealth.LOGGER.debug("Getting damage tier for entity ID {}: {}", entityId, tier);
        }

        return tier;
    }

    // Calculate damage tier based on health percentage
    private static int calculateDamageTier(LivingEntity entity) {
        float healthPercent = entity.getHealth() / entity.getMaxHealth();

        if (healthPercent >= 1.0f) return 0;  // 100% health - No damage
        if (healthPercent > 0.8f) return 1;   // 99-80% health - Light scratches (6 wounds)
        if (healthPercent > 0.6f) return 2;   // 79-60% health - Moderate cuts (12 wounds)
        if (healthPercent > 0.4f) return 3;   // 59-40% health - Heavy wounds (18 wounds)
        if (healthPercent > 0.2f) return 4;   // 39-20% health - Severe wounds (24 wounds)
        return 5;                              // 19-0% health - Critical (30 wounds)
    }

    // Set the damage type that caused a specific tier for an entity
    // Called by DamageEventHandler when entity takes damage
    public static void setDamageTypeForTier(int entityId, int tier, DamageType damageType) {
        ENTITY_TIER_DAMAGE_TYPES.computeIfAbsent(entityId, k -> new ConcurrentHashMap<>()).put(tier, damageType);

        if (VisualHealth.debugMode) {
            VisualHealth.LOGGER.debug("Set damage type {} for entity ID {} tier {}",
                    damageType, entityId, tier);
        }
    }

    // Get the damage type that caused a specific tier for an entity
    // Returns SWORD if no damage type was recorded (game reload scenario)
    // This ensures wounds use proper colors (entity override or config color)
    public static DamageType getDamageTypeForTier(int entityId, int tier) {
        Map<Integer, DamageType> tierMap = ENTITY_TIER_DAMAGE_TYPES.get(entityId);
        if (tierMap == null) {
            // No damage type data (game reload or new entity)
            // Default to SWORD so wounds use proper colors (override or config)
            return DamageType.SWORD;
        }
        return tierMap.getOrDefault(tier, DamageType.SWORD);
    }

    // Clear damage types for all tiers above the specified tier
    // Called when entity heals and loses damage tiers
    private static void clearTiersAbove(int entityId, int maxTier) {
        Map<Integer, DamageType> tierMap = ENTITY_TIER_DAMAGE_TYPES.get(entityId);
        if (tierMap != null) {
            // Remove all tiers greater than maxTier
            tierMap.keySet().removeIf(tier -> tier > maxTier);
        }

        // Clear texture cache for this entity's tiers above maxTier
        win.demistorm.visual_health.client.texture.WoundTextureGenerator.clearEntityTiers(entityId, maxTier + 1, 5);
        win.demistorm.visual_health.client.texture.EMFDamageTextureGenerator.clearEntityTiers(entityId, maxTier + 1, 5);
    }
}
