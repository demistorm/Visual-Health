package win.demistorm.visual_health.client.texture;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import win.demistorm.visual_health.VisualHealth;
import win.demistorm.visual_health.client.DamageType;
import win.demistorm.visual_health.client.EntityHealthTracker;
import win.demistorm.visual_health.client.TintCalculator;
import win.demistorm.visual_health.client.renderer.WoundAssetSelector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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

    // Base reference texture size for wound count scaling
    private static final int BASE_TEXTURE_SIZE = 64;

    /**
     * Generate a damaged version of an EMF variant texture.
     *
     * @param variantTexture The EMF variant texture (e.g., creeper_rock)
     * @param entity The entity being damaged
     * @param damageTier The damage tier (1-5)
     * @return Identifier for the damaged variant texture
     */
    public static Identifier generateDamagedVariant(
            Identifier variantTexture,
            LivingEntity entity,
            int damageTier
    ) {
        // Build cache key that includes damage type history
        // This ensures different weapon combinations get different cached textures
        // Format: variantTexture_tier#_entityId_weapon1_weapon2_...
        StringBuilder cacheKeyBuilder = new StringBuilder();
        cacheKeyBuilder.append(variantTexture.toString()).append("_tier").append(damageTier).append("_entity").append(entity.getId());

        // Include damage type for each tier in the cache key
        for (int tier = 1; tier <= damageTier; tier++) {
            DamageType damageType = EntityHealthTracker.getDamageTypeForTier(entity.getId(), tier);
            cacheKeyBuilder.append("_").append(damageType.name());
        }

        String cacheKey = cacheKeyBuilder.toString();

        // Check cache
        if (DAMAGE_CACHE.containsKey(cacheKey)) {
            return DAMAGE_CACHE.get(cacheKey);
        }

        try {
            if (VisualHealth.debugMode) {
                VisualHealth.LOGGER.debug("Generating EMF damaged variant: {} (tier {})",
                        variantTexture, damageTier);
            }

            // Load the variant texture
            net.minecraft.server.packs.resources.ResourceManager resourceManager =
                    net.minecraft.client.Minecraft.getInstance().getResourceManager();

            // Check if texture exists in resource pack (skip if it's a dynamic texture we already generated)
            if (variantTexture.getNamespace().equals("visualhealth") &&
                    variantTexture.getPath().startsWith("dynamic/emf_damage/")) {
                // This is one of our dynamically generated textures, already in cache
                if (VisualHealth.debugMode) {
                    VisualHealth.LOGGER.debug("Skipping generation for existing dynamic texture: {}", variantTexture);
                }
                return variantTexture;
            }

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

            // Calculate wound count based on texture area (scaling) and density percentage
            double areaScale = (variantImage.getWidth() * variantImage.getHeight()) /
                    (double) (BASE_TEXTURE_SIZE * BASE_TEXTURE_SIZE);

            // Base wound count from percentage (10-100% scales linearly 9-93 wounds per tier at 64x64)
            // 10% = 9 wounds per tier, 100% = 96 wounds per tier (384 wounds at tier 4)
            int densityPercent = win.demistorm.visual_health.ConfigHelper.INSTANCE.woundDensityPercentage;
            int baseWoundsPerTier = (densityPercent * 96) / 100;

            // Scale by texture area so larger mobs get proportionally more wounds
            int woundsPerTier = (int) (baseWoundsPerTier * areaScale);

            if (VisualHealth.debugMode) {
                VisualHealth.LOGGER.debug("EMF generation: area scale={:.2f}, wounds per tier={}",
                        areaScale, woundsPerTier);
            }

            // Track wound positions for rejection sampling (reset per tier)
            List<int[]> rejectedPositions = new ArrayList<>();

            // Stamp wound textures onto the variant
            int woundIndex = 0;
            for (int tier = 1; tier <= damageTier; tier++) {
                // Clear position tracking for each tier (allow overlap between tiers)
                rejectedPositions.clear();

                // Get the damage type that caused this tier
                DamageType damageType = EntityHealthTracker.getDamageTypeForTier(entity.getId(), tier);

                // Create a tier-specific random seed to ensure wounds stay in consistent positions
                // The seed includes the weapon sequence up to this tier, so tier 1-2 wounds
                // are in the same positions whether viewing tier 2 or tier 3
                long tierSeed = entity.getId();
                for (int t = 1; t <= tier; t++) {
                    DamageType dt = EntityHealthTracker.getDamageTypeForTier(entity.getId(), t);
                    tierSeed = tierSeed * 31 + dt.name().hashCode();
                }
                Random tierRandom = new Random(tierSeed);

                // Calculate minimum distance for THIS TIER based on fixed per-tier wound count
                // This ensures each tier uses consistent spacing regardless of viewing tier 2 or tier 3
                int tierWounds = woundsPerTier * 1;  // Just this tier's wounds
                int tierDistanceFactor = 6 + (tierWounds / 15);
                int tierMinDistance = Math.max(4, (int) (Math.sqrt(variantImage.getWidth() * variantImage.getHeight()) / tierDistanceFactor * 0.3));

                // Get the appropriate tint for this damage type
                // NOTE: This replaces the deprecated tint parameter
                int woundTint = TintCalculator.getTintForDamageType(damageType, entity);

                if (VisualHealth.debugMode) {
                    VisualHealth.LOGGER.debug("Tier {}: damage type={}, tint=0x{}",
                            tier, damageType, Integer.toHexString(woundTint));
                }

                // Stamp wounds for this tier
                for (int i = 0; i < woundsPerTier; i++) {
                    try {
                        // Get a random wound texture for this damage type
                        Identifier woundAssetId = WoundAssetSelector.getRandomWoundTexture(damageType, tierRandom);

                        // Load the wound texture
                        NativeImage woundAsset;
                        try (var resource = resourceManager.open(woundAssetId)) {
                            woundAsset = NativeImage.read(resource);
                        }

                        // CRITICAL: Apply tint to wound BEFORE stamping
                        // Use shared TintUtils.applyTint() instead of local method
                        NativeImage tintedWound = TintUtils.applyTint(woundAsset, woundTint);

                        // Find position using rejection sampling for even distribution
                        int[] position = findValidPosition(variantImage.getWidth(), variantImage.getHeight(),
                                tintedWound.getWidth(), tintedWound.getHeight(), rejectedPositions, tierMinDistance, tierRandom);

                        if (VisualHealth.debugMode) {
                            VisualHealth.LOGGER.debug("Stamping wound {} (tier {}, {}) at ({}, {})",
                                    ++woundIndex, tier, damageType, position[0], position[1]);
                        }

                        // Stamp the tinted wound onto the variant texture
                        stampTexture(damagedVariant, tintedWound, position[0], position[1]);

                        // Clean up tinted wound
                        tintedWound.close();
                        woundAsset.close();

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
                    dynamicTextureId::toString,
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

    // Find a valid position for a wound using rejection sampling
    // Ensures wounds are evenly distributed by maintaining minimum distance
    // Returns [x, y] position
    private static int[] findValidPosition(int textureWidth, int textureHeight, int woundWidth, int woundHeight,
                                           List<int[]> existingPositions, int minDistance, Random random) {
        int maxAttempts = 100; // Prevent infinite loop
        int attempt = 0;

        while (attempt < maxAttempts) {
            // Generate random position
            int maxX = textureWidth - woundWidth;
            int maxY = textureHeight - woundHeight;
            int x = random.nextInt(Math.max(1, maxX));
            int y = random.nextInt(Math.max(1, maxY));

            // Check if position is valid (not too close to existing wounds)
            boolean isValid = true;
            for (int[] existing : existingPositions) {
                double distance = Math.sqrt(Math.pow(x - existing[0], 2) + Math.pow(y - existing[1], 2));
                if (distance < minDistance) {
                    isValid = false;
                    break;
                }
            }

            if (isValid) {
                // Found a valid position
                int[] position = new int[]{x, y};
                existingPositions.add(position);
                return position;
            }

            attempt++;
        }

        // Couldn't find ideal position after max attempts, use last random position
        int x = random.nextInt(Math.max(1, textureWidth - woundWidth));
        int y = random.nextInt(Math.max(1, textureHeight - woundHeight));
        int[] position = new int[]{x, y};
        existingPositions.add(position);
        return position;
    }

    // Clear texture cache for specific entity tiers
    // Called when entity heals and loses damage tiers
    public static void clearEntityTiers(int entityId, int minTier, int maxTier) {
        int cleared = 0;
        List<String> keysToRemove = new ArrayList<>();

        for (String key : DAMAGE_CACHE.keySet()) {
            if (key.contains("_entity" + entityId + "_")) {
                // Extract tier number from key (format: variantTexture_tier#_entityId_weapon1_weapon2_...)
                // Find the position of "_tier" and "_entity"
                int tierStartIndex = key.indexOf("_tier");
                if (tierStartIndex == -1) {
                    continue; // Invalid key format
                }

                // Find the underscore after the tier number (before "_entity")
                int tierEndIndex = key.indexOf("_entity", tierStartIndex);
                if (tierEndIndex == -1) {
                    continue; // Invalid key format
                }

                String tierStr = key.substring(tierStartIndex + 5, tierEndIndex); // +5 to skip "_tier"
                try {
                    int tier = Integer.parseInt(tierStr);
                    if (tier >= minTier && tier <= maxTier) {
                        keysToRemove.add(key);
                    }
                } catch (NumberFormatException e) {
                    // Invalid key format, skip
                }
            }
        }

        for (String key : keysToRemove) {
            DAMAGE_CACHE.remove(key);
            cleared++;
        }

        if (cleared > 0 && VisualHealth.debugMode) {
            VisualHealth.LOGGER.debug("Cleared {} EMF damage texture cache entries for entity ID {} (tiers {}-{})",
                    cleared, entityId, minTier, maxTier);
        }
    }

    // Clear all cached textures
    // Called on resource reload to invalidate all texture identifiers
    public static void clearAllCaches() {
        int cacheSize = DAMAGE_CACHE.size();
        DAMAGE_CACHE.clear();

        if (cacheSize > 0) {
            VisualHealth.LOGGER.info("Cleared {} EMF damage texture cache entries on resource reload", cacheSize);
        }
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
                int blendedPixel = getBlendedPixel(basePixel, stampAlpha, stampPixel);
                baseTexture.setPixel(baseX, baseY, blendedPixel);
            }
        }
    }

    private static int getBlendedPixel(int basePixel, int stampAlpha, int stampPixel) {
        int baseAlpha = (basePixel >> 24) & 0xFF;

        // Simple alpha blending (stamp overlays base)
        float alphaRatio = stampAlpha / 255.0f;
        int blendedR = blendChannel((basePixel >> 16) & 0xFF, (stampPixel >> 16) & 0xFF, alphaRatio);
        int blendedG = blendChannel((basePixel >> 8) & 0xFF, (stampPixel >> 8) & 0xFF, alphaRatio);
        int blendedB = blendChannel(basePixel & 0xFF, stampPixel & 0xFF, alphaRatio);
        int blendedA = Math.min(255, baseAlpha + stampAlpha);

        return (blendedA << 24) | (blendedR << 16) | (blendedG << 8) | blendedB;
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

}
