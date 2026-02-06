package win.demistorm.visual_health.client;

import net.minecraft.client.model.EntityModel;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import traben.entity_model_features.models.IEMFModel;
import win.demistorm.visual_health.VisualHealth;
import win.demistorm.visual_health.client.texture.EMFDamageTextureGenerator;

/**
 * Helper class for applying damage to EMF variant textures.
 * Detects EMF models with texture overrides and replaces them with damaged versions.
 */
public final class EMFDamageHelper {

    private EMFDamageHelper() {
        // Utility class - no instances
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
                Identifier variantTexture = part.textureOverride;

                // Generate damaged variant texture
                Identifier damagedVariant = EMFDamageTextureGenerator.generateDamagedVariant(
                        variantTexture, entity, damageTier, tint);

                // Replace the override with the damaged version
                part.textureOverride = damagedVariant;

                if (VisualHealth.debugMode) {
                    VisualHealth.LOGGER.debug("Replaced override {} with {} for part {}",
                            variantTexture, damagedVariant, part.toStringShort());
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
}
