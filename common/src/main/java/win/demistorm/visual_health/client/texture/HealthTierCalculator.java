package win.demistorm.visual_health.client.texture;

import net.minecraft.world.entity.LivingEntity;

// Calculates damage tier (0-4) based on entity health percentage
public class HealthTierCalculator {

    // Health percentage thresholds for each damage tier
    private static final float TIER_1_THRESHOLD = 0.8f; // 80% - light scratches
    private static final float TIER_2_THRESHOLD = 0.6f; // 60% - moderate cuts
    private static final float TIER_3_THRESHOLD = 0.4f; // 40% - heavy wounds
    private static final float TIER_4_THRESHOLD = 0.2f; // 20% - critical damage

    // Get damage tier for an entity (0-4, where 0 is no damage)
    public static int getDamageTier(LivingEntity entity) {
        float healthPercent = entity.getHealth() / entity.getMaxHealth();

        if (healthPercent > TIER_1_THRESHOLD) {
            return 0; // No damage needed
        } else if (healthPercent > TIER_2_THRESHOLD) {
            return 1; // Light scratches
        } else if (healthPercent > TIER_3_THRESHOLD) {
            return 2; // Moderate cuts
        } else if (healthPercent > TIER_4_THRESHOLD) {
            return 3; // Heavy wounds
        } else {
            return 4; // Critical damage
        }
    }

    // Calculate number of wounds to apply based on damage tier
    // More wounds at higher damage tiers for visual progression
    public static int getWoundCount(int damageTier) {
        return damageTier * 3 + 2; // Tier 0: 2, Tier 1: 5, Tier 2: 8, Tier 3: 11, Tier 4: 14
    }
}
