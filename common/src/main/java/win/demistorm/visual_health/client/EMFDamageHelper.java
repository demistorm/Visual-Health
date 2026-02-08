package win.demistorm.visual_health.client;

import net.minecraft.client.model.EntityModel;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import traben.entity_model_features.models.IEMFModel;
import traben.entity_model_features.models.parts.EMFModelPart;
import traben.entity_texture_features.ETFApi;
import win.demistorm.visual_health.VisualHealth;
import win.demistorm.visual_health.client.texture.EMFDamageTextureGenerator;

/**
 * Helper class for applying damage to EMF variant textures.
 * Detects EMF models with texture overrides and generates per-entity wound textures.
 *
 * Uses per-entity texture registration instead of modifying shared model parts,
 * ensuring each entity gets its correct wound texture without affecting others.
 */
public final class EMFDamageHelper {

    private EMFDamageHelper() {
        // Utility class - no instances
    }

    /**
     * Check if the entity has an EMF model with texture overrides, and if so, register wound textures.
     *
     * Instead of modifying the shared EMF model part's textureOverride field (which would affect
     * all entities of the same type), we generate wound textures and register them per-entity.
     * The render mixin will swap these textures in at render time.
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

        // Generate wound texture for the first part with texture override
        // All parts with textureOverride will use the same variant texture
        Identifier woundTexture = null;
        for (var part : emfRoot.getAllVanillaPartsEMF()) {
            if (part.textureOverride != null) {
                // Get ETF's variant of EMF's texture (handles variants, emissives, etc.)
                Identifier etfProcessedTexture = ETFApi.getCurrentETFVariantTextureOfEntity(
                        entity, part.textureOverride);

                // Generate wound texture from the ETF-processed texture
                woundTexture = EMFDamageTextureGenerator.generateDamagedVariant(
                        etfProcessedTexture, entity, damageTier, tint);
                break; // Only need to generate once
            }
        }

        if (woundTexture != null) {
            // Register this wound texture for this specific entity by its ID
            // Using entity ID instead of UUID for better EMF compatibility
            EMFPerEntityTextures.setWoundTextureById(String.valueOf(entity.getId()), woundTexture);

            if (VisualHealth.debugMode) {
                VisualHealth.LOGGER.debug("Registered wound texture {} for entity ID {}",
                        woundTexture, entity.getId());
            }

            return true; // Damage applied, skip normal overlay render
        }

        return false;
    }

    /**
     * Clear the per-entity texture mappings.
     * Should be called when resources are reloaded.
     */
    public static void clearCache() {
        EMFPerEntityTextures.clearAll();
    }
}
