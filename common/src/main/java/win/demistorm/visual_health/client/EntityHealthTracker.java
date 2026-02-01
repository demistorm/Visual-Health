package win.demistorm.visual_health.client;

import net.minecraft.world.entity.LivingEntity;
import win.demistorm.visual_health.VisualHealth;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// Track damage tiers for entities by their ID
// In 1.21.11+, render state doesn't contain health, so we track it separately
public final class EntityHealthTracker {

    private EntityHealthTracker() {
        // Utility class - no instances
    }

    // Map entity ID -> damage tier (0-4)
    private static final Map<Integer, Integer> ENTITY_DAMAGE_TIERS = new ConcurrentHashMap<>();

    // Update the damage tier for an entity
    // Called during extractRenderState when entity data is fresh
    public static void updateEntityDamageTier(LivingEntity entity) {
        if (entity == null) {
            return;
        }

        int tier = calculateDamageTier(entity);
        int oldTier = ENTITY_DAMAGE_TIERS.getOrDefault(entity.getId(), 0);
        ENTITY_DAMAGE_TIERS.put(entity.getId(), tier);

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

    // Remove entity from tracker when it's removed from world
    public static void removeEntity(int entityId) {
        if (ENTITY_DAMAGE_TIERS.containsKey(entityId)) {
            ENTITY_DAMAGE_TIERS.remove(entityId);
            VisualHealth.LOGGER.debug("Removed entity ID {} from damage tracker", entityId);
        }
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

    // Clear all tracked entities (call on resource reload)
    public static void clearAll() {
        int count = ENTITY_DAMAGE_TIERS.size();
        ENTITY_DAMAGE_TIERS.clear();
        VisualHealth.LOGGER.info("Cleared all entity damage tiers ({} entities)", count);
    }
}
