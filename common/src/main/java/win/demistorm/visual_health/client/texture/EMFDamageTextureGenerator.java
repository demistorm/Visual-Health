package win.demistorm.visual_health.client.texture;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import traben.entity_model_features.models.parts.EMFModelPart;
import win.demistorm.visual_health.VisualHealth;
import win.demistorm.visual_health.client.renderer.WoundAssetSelector;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Generates damaged versions of EMF variant textures by stamping wound decals onto them.
 * Caches generated textures to avoid regenerating every frame.
 */
public class EMFDamageTextureGenerator {

    private EMFDamageTextureGenerator() {
        // Utility class - no instances
    }

    // Cache generated damaged variant textures
    // Key: variantTexture + damageTier + entityUUID
    // Value: Dynamic texture identifier
    private static final Map<String, Identifier> DAMAGE_CACHE = new HashMap<>();

    /**
     * Generate a damaged version of an EMF variant texture.
     *
     * @param variantTexture The EMF variant texture (e.g., creeper_rock)
     * @param entity The entity being damaged
     * @param damageTier The damage tier (1-5)
     * @param tint The tint color to apply to wounds (ARGB format)
     * @return Identifier for the damaged variant texture
     */
    public static Identifier generateDamagedVariant(
            Identifier variantTexture,
            LivingEntity entity,
            int damageTier,
            int tint
    ) {
        // Create cache key
        String cacheKey = variantTexture.toString() + "_tier" + damageTier + "_" + entity.getUUID();

        // Check cache
        if (DAMAGE_CACHE.containsKey(cacheKey)) {
            return DAMAGE_CACHE.get(cacheKey);
        }

        try {
            if (VisualHealth.debugMode) {
                VisualHealth.LOGGER.debug("Generating EMF damaged variant: {} (tier {}, tint 0x{})",
                        variantTexture, damageTier, Integer.toHexString(tint));
            }

            // Load the variant texture
            net.minecraft.server.packs.resources.ResourceManager resourceManager =
                    net.minecraft.client.Minecraft.getInstance().getResourceManager();

            NativeImage variantImage;
            try (var resource = resourceManager.open(variantTexture)) {
                variantImage = NativeImage.read(resource);
            }

            // Create a copy to modify (don't modify the original)
            NativeImage damagedVariant = new NativeImage(variantImage.getWidth(), variantImage.getHeight(), true);
            for (int y = 0; y < variantImage.getHeight(); y++) {
                for (int x = 0; x < variantImage.getWidth(); x++) {
                    damagedVariant.setPixel(x, y, variantImage.getPixel(x, y));
                }
            }

            // Calculate number of wounds based on damage tier
            int woundCount = damageTier * win.demistorm.visual_health.ConfigHelper.INSTANCE.woundsPerTier;

            // Use entity UUID for consistent random seed
            Random random = new Random(entity.getUUID().getLeastSignificantBits());

            // Stamp wound textures onto the variant
            int woundIndex = 0;
            int woundsPerTier = win.demistorm.visual_health.ConfigHelper.INSTANCE.woundsPerTier;
            for (int tier = 1; tier <= damageTier; tier++) {
                for (int i = 0; i < woundsPerTier; i++) {
                    try {
                        // Get a random wound texture for this tier
                        Identifier woundAssetId = WoundAssetSelector.getRandomWoundTexture(tier, random);

                        // Load the wound texture
                        NativeImage woundAsset;
                        try (var resource = resourceManager.open(woundAssetId)) {
                            woundAsset = NativeImage.read(resource);
                        }

                        // Apply tint to wound texture
                        NativeImage tintedWound = applyTint(woundAsset, tint);

                        // Random position for this wound (stay within bounds)
                        int maxX = damagedVariant.getWidth() - tintedWound.getWidth();
                        int maxY = damagedVariant.getHeight() - tintedWound.getHeight();
                        int posX = random.nextInt(Math.max(1, maxX));
                        int posY = random.nextInt(Math.max(1, maxY));

                        if (VisualHealth.debugMode) {
                            VisualHealth.LOGGER.debug("Stamping wound {} (tier {}) at ({}, {})",
                                    ++woundIndex, tier, posX, posY);
                        }

                        // Stamp the tinted wound onto the variant texture
                        stampTexture(damagedVariant, tintedWound, posX, posY);

                        // Clean up tinted wound
                        tintedWound.close();

                    } catch (Exception e) {
                        VisualHealth.LOGGER.error("Failed to load or stamp wound texture: {}", e.getMessage(), e);
                    }
                }
            }

            // Register the damaged variant as a dynamic texture
            TextureManager textureManager = net.minecraft.client.Minecraft.getInstance().getTextureManager();
            Identifier dynamicTextureId = Identifier.fromNamespaceAndPath("visualhealth",
                    "dynamic/emf_damage/" + entity.getId() + "/" + variantTexture.getPath().replace('/', '_') + "_tier" + damageTier);

            if (VisualHealth.debugMode) {
                VisualHealth.LOGGER.debug("Registering EMF damaged variant texture: {}", dynamicTextureId);
            }

            DynamicTexture texture = new DynamicTexture(
                    () -> dynamicTextureId.toString(),
                    damagedVariant
            );

            textureManager.register(dynamicTextureId, texture);

            // Cache the result
            DAMAGE_CACHE.put(cacheKey, dynamicTextureId);

            // Clean up the original variant image
            variantImage.close();

            return dynamicTextureId;

        } catch (Exception e) {
            VisualHealth.LOGGER.error("Failed to generate EMF damaged variant: {}", e.getMessage(), e);
            return variantTexture; // Fallback to original variant
        }
    }

    /**
     * Apply a tint color to a wound texture.
     * Tints the RGB channels while preserving alpha.
     *
     * @param wound The wound texture to tint
     * @param tint The tint color (ARGB format)
     * @return A new tinted wound texture
     */
    private static NativeImage applyTint(NativeImage wound, int tint) {
        // Extract tint components
        int tintR = (tint >> 16) & 0xFF;
        int tintG = (tint >> 8) & 0xFF;
        int tintB = tint & 0xFF;

        // Create a new image for the tinted wound
        NativeImage tinted = new NativeImage(wound.getWidth(), wound.getHeight(), true);

        for (int y = 0; y < wound.getHeight(); y++) {
            for (int x = 0; x < wound.getWidth(); x++) {
                int pixel = wound.getPixel(x, y);
                int alpha = (pixel >> 24) & 0xFF;

                // Skip fully transparent pixels
                if (alpha == 0) {
                    continue;
                }

                // Get original RGB
                int r = (pixel >> 16) & 0xFF;
                int g = (pixel >> 8) & 0xFF;
                int b = pixel & 0xFF;

                // Apply tint using multiply blending
                // This preserves the wound details while applying the color
                int tintedR = (r * tintR) / 255;
                int tintedG = (g * tintG) / 255;
                int tintedB = (b * tintB) / 255;

                // Combine with original alpha
                int tintedPixel = (alpha << 24) | (tintedR << 16) | (tintedG << 8) | tintedB;
                tinted.setPixel(x, y, tintedPixel);
            }
        }

        return tinted;
    }

    /**
     * Stamp a texture onto a base texture at the specified position.
     * Uses alpha blending for smooth edges.
     *
     * @param baseTexture The base texture to stamp onto
     * @param stamp The stamp texture
     * @param posX X position to stamp at
     * @param posY Y position to stamp at
     */
    private static void stampTexture(NativeImage baseTexture, NativeImage stamp, int posX, int posY) {
        for (int y = 0; y < stamp.getHeight(); y++) {
            for (int x = 0; x < stamp.getWidth(); x++) {
                // Check bounds
                if (posX + x >= baseTexture.getWidth() || posY + y >= baseTexture.getHeight()) {
                    continue;
                }

                // Get pixel from stamp texture (returns ARGB format)
                int stampPixel = stamp.getPixel(x, y);
                int stampAlpha = (stampPixel >> 24) & 0xFF;

                // Skip fully transparent pixels
                if (stampAlpha == 0) {
                    continue;
                }

                // Get existing pixel from base texture
                int baseX = posX + x;
                int baseY = posY + y;
                int basePixel = baseTexture.getPixel(baseX, baseY);
                int baseAlpha = (basePixel >> 24) & 0xFF;

                // Simple alpha blending (stamp overlays base)
                float alphaRatio = stampAlpha / 255.0f;
                int blendedR = blendChannel((basePixel >> 16) & 0xFF, (stampPixel >> 16) & 0xFF, alphaRatio);
                int blendedG = blendChannel((basePixel >> 8) & 0xFF, (stampPixel >> 8) & 0xFF, alphaRatio);
                int blendedB = blendChannel(basePixel & 0xFF, stampPixel & 0xFF, alphaRatio);
                int blendedA = Math.min(255, baseAlpha + stampAlpha);

                int blendedPixel = (blendedA << 24) | (blendedR << 16) | (blendedG << 8) | blendedB;
                baseTexture.setPixel(baseX, baseY, blendedPixel);
            }
        }
    }

    /**
     * Blend two color channels based on alpha ratio.
     *
     * @param base The base channel value
     * @param stamp The stamp channel value
     * @param alphaRatio The alpha ratio (0.0 to 1.0)
     * @return The blended channel value
     */
    private static int blendChannel(int base, int stamp, float alphaRatio) {
        return (int)(base * (1.0f - alphaRatio) + stamp * alphaRatio);
    }

    /**
     * Clear the damage cache (e.g., when resources are reloaded).
     */
    public static void clearCache() {
        DAMAGE_CACHE.clear();
        if (VisualHealth.debugMode) {
            VisualHealth.LOGGER.debug("EMF damage texture cache cleared");
        }
    }
}
