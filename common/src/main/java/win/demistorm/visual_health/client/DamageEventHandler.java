package win.demistorm.visual_health.client;

import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.damagesource.DamageSource;
import win.demistorm.visual_health.VisualHealth;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// Handles client-side damage events to track what weapons are being used
// Called from platform-specific event registration (Fabric/Forge/NeoForge)
public final class DamageEventHandler {

    private DamageEventHandler() {
        // Utility class - no instances
    }

    // Track the last damage type that hit each entity
    // Used by EntityHealthTracker when a tier increases
    private static final Map<Integer, DamageType> LAST_DAMAGE_TYPE = new ConcurrentHashMap<>();

    // Get the last damage type that hit an entity
    // Called by EntityHealthTracker when assigning damage types to tiers
    // Defaults to SWORD for better visual consistency after game reload
    public static DamageType getLastDamageType(int entityId) {
        return LAST_DAMAGE_TYPE.getOrDefault(entityId, DamageType.SWORD);
    }

    // Clear the last damage type for an entity
    // Called when entity is removed from world
    public static void clearLastDamageType(int entityId) {
        LAST_DAMAGE_TYPE.remove(entityId);
    }

    // Called when a living entity is damaged on the client
    // Detects the weapon type and updates the damage tracker
    public static void onLivingDamage(LivingEntity entity, DamageSource source) {
        if (entity == null) {
            return;
        }

        // Detect the damage type from the damage source
        DamageType damageType = detectDamageType(source);

        // Store this damage type for when the tier update happens
        // (EntityHealthTracker will call getLastDamageType when tier changes)
        int entityId = entity.getId();
        LAST_DAMAGE_TYPE.put(entityId, damageType);

        if (VisualHealth.debugMode) {
            int currentTier = EntityHealthTracker.getDamageTier(entityId);
            VisualHealth.LOGGER.debug("Entity {} (ID: {}) damaged by {}, current tier: {}",
                    entity.getName().getString(), entityId, damageType, currentTier);
        }
    }

    // Detect the type of damage from a DamageSource
    private static DamageType detectDamageType(DamageSource source) {
        // Check if damage was caused by a player holding an item
        if (source.getEntity() instanceof Player attacker) {
            ItemStack weapon = attacker.getMainHandItem();

            if (weapon.isEmpty()) {
                return DamageType.GENERIC; // Empty hand = generic (punches)
            }

            // Use ItemTags for weapon detection (1.21.11+ API)
            if (weapon.is(ItemTags.SWORDS)) {
                return DamageType.SWORD;
            } else if (weapon.is(ItemTags.AXES)) {
                return DamageType.AXE;
            } else if (weapon.is(ItemTags.TRIDENT_ENCHANTABLE)) {
                return DamageType.TRIDENT;
            } else if (weapon.is(ItemTags.SPEARS)) {
                return DamageType.SPEAR; // Added in 1.21.11!
            }

            // Holding something but not a recognized weapon
            return DamageType.GENERIC;
        }

        // All other damage sources (arrows, fire, fall, magic, etc.) use GENERIC
        return DamageType.GENERIC;
    }
}
