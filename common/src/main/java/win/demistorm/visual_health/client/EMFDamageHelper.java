package win.demistorm.visual_health.client;

import net.minecraft.client.model.EntityModel;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import traben.entity_model_features.models.IEMFModel;
import traben.entity_model_features.models.parts.EMFModelPart;
import win.demistorm.visual_health.VisualHealth;
import win.demistorm.visual_health.client.texture.EMFDamageTextureGenerator;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Helper class for applying damage to EMF variant textures.
 * Detects EMF models with texture overrides and replaces them with damaged versions.
 * Preserves original variant textures to allow damage tier updates.
 */
public final class EMFDamageHelper {

    private EMFDamageHelper() {
        // Utility class - no instances
    }

    // Track original variant textures before we replace them
    // Uses WeakHashMap for automatic cleanup when model parts are garbage collected
    private static final Map<EMFModelPart, Identifier> ORIGINAL_VARIANTS = new WeakHashMap<>();

    /**
     * Store the original variant texture for a model part.
     *
     * @param part The EMF model part
     * @param originalVariant The original texture override
     */
    private static void storeOriginalVariant(EMFModelPart part, Identifier originalVariant) {
        ORIGINAL_VARIANTS.put(part, originalVariant);
        if (VisualHealth.debugMode) {
            VisualHealth.LOGGER.debug("Stored original variant {} for part {}",
                    originalVariant, part.toStringShort());
        }
    }

    /**
     * Get the original variant texture for a model part.
     *
     * @param part The EMF model part
     * @return The original variant, or null if not stored
     */
    private static Identifier getOriginalVariant(EMFModelPart part) {
        return ORIGINAL_VARIANTS.get(part);
    }

    /**
     * Check if the entity has an EMF model with texture overrides, and if so, apply damage.
     *
     * @param model The entity model
     * @param entity The entity being rendered
     * @param damageTier The current damage tier
     * @return true if EMF damage was applied (caller should skip normal overlay render)
     */
    public static boolean applyEMFDamageIfPresent(
            EntityModel<?> model,
            LivingEntity entity,
            int damageTier
    ) {
        // Check if this is an EMF model
        if (!(model instanceof IEMFModel emfModel)) {
            return false;
        }

        var emfRoot = emfModel.emf$getEMFRootModel();

        // Check if any parts have texture overrides
        boolean hasOverrides = false;
        for (var part : emfRoot.getAllVanillaPartsEMF()) {
            if (part.textureOverride != null) {
                hasOverrides = true;
                break;
            }
        }

        if (!hasOverrides) {
            if (VisualHealth.debugMode) {
                VisualHealth.LOGGER.debug("EMF model detected but no texture overrides for {}",
                        entity.getName().getString());
            }
            return false;
        }

        if (VisualHealth.debugMode) {
            VisualHealth.LOGGER.debug("Applying EMF damage for {} (tier {})",
                    entity.getName().getString(), damageTier);
        }

        // Determine tint color (same logic as DamageOverlayLayer)
        int tint;
        EntityDamageColors.DamageOverride override =
                EntityDamageColors.getOverride(entity.getType());

        if (override != null) {
            // Use entity-specific override
            tint = override.tintColor();

            if (VisualHealth.debugMode) {
                VisualHealth.LOGGER.debug("Using entity override tint: 0x{}",
                        Integer.toHexString(tint));
            }
        } else {
            // Use default config color
            tint = switch (win.demistorm.visual_health.ConfigHelper.INSTANCE.damageColor) {
                case RED -> 0xFF9F0000;   // Blood red
                case BLACK -> 0xFF000000; // Black
                case WHITE -> 0xFFFFFFFF; // Pure white
            };

            if (VisualHealth.debugMode) {
                VisualHealth.LOGGER.debug("Using default config tint: 0x{}",
                        Integer.toHexString(tint));
            }
        }

        // Apply damage to all parts with texture overrides
        int partsModified = 0;
        for (var part : emfRoot.getAllVanillaPartsEMF()) {
            if (part.textureOverride != null) {
                // Get or store the original variant texture
                Identifier originalVariant = getOriginalVariant(part);

                if (originalVariant == null) {
                    // First time seeing this part - store the original variant
                    originalVariant = part.textureOverride;
                    storeOriginalVariant(part, originalVariant);

                    if (VisualHealth.debugMode) {
                        VisualHealth.LOGGER.debug("Stored original variant {} for part {}",
                                originalVariant, part.toStringShort());
                    }
                }

                // Always generate damage from the ORIGINAL variant, not the current override
                // This prevents trying to load our damaged textures as the base for new damage
                Identifier damagedVariant = EMFDamageTextureGenerator.generateDamagedVariant(
                        originalVariant, entity, damageTier, tint);

                // Replace the override with the damaged version
                part.textureOverride = damagedVariant;

                if (VisualHealth.debugMode) {
                    VisualHealth.LOGGER.debug("Replaced override {} with damaged variant {} (from original {}) for part {}",
                            part.textureOverride, damagedVariant, originalVariant, part.toStringShort());
                }

                partsModified++;
            }
        }

        if (VisualHealth.debugMode) {
            VisualHealth.LOGGER.debug("EMF damage applied to {} parts for {}",
                    partsModified, entity.getName().getString());
        }

        return true; // Damage applied, skip normal overlay render
    }

    /**
     * Clear the original variants cache.
     * Should be called when resources are reloaded or when needed to free memory.
     */
    public static void clearCache() {
        ORIGINAL_VARIANTS.clear();
        if (VisualHealth.debugMode) {
            VisualHealth.LOGGER.debug("EMF original variants cache cleared");
        }
    }
}
