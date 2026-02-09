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

// Generates composite textures with wound effects stamped onto them
// Creates entity-sized textures with wound overlays for the RenderLayer system
public class WoundTextureGenerator {

    private WoundTextureGenerator() {
        // Utility class - no instances
    }

    // Cache generated wounded textures
    // Key: entityId + damageTier
    // Value: Dynamic texture identifier
    private static final Map<String, Identifier> WOUND_CACHE = new HashMap<>();

    // Base reference texture size for wound count scaling
    private static final int BASE_TEXTURE_SIZE = 64;

    // Generate a texture with wounds stamped onto it
    // Returns a texture identifier that can be used with entity models
    public static Identifier generateWoundedTexture(LivingEntity entity, int damageTier, int baseWidth, int baseHeight) {
        // Check cache first
        String cacheKey = entity.getId() + "_tier" + damageTier;
        if (WOUND_CACHE.containsKey(cacheKey)) {
            return WOUND_CACHE.get(cacheKey);
        }

        try {
            // Create a new NativeImage with RGBA format (supports transparency)
            NativeImage woundTexture = new NativeImage(baseWidth, baseHeight, true);

            // Fill with transparent black (fully transparent) - ARGB format
            int transparent = 0x00000000; // A=0, R=0, G=0, B=0
            for (int y = 0; y < baseHeight; y++) {
                for (int x = 0; x < baseWidth; x++) {
                    woundTexture.setPixel(x, y, transparent);
                }
            }

            // Calculate wound count based on texture area (scaling) and density percentage
            double areaScale = (baseWidth * baseHeight) / (double) (BASE_TEXTURE_SIZE * BASE_TEXTURE_SIZE);

            // Base wound count from percentage (10-100% scales to 3-12 wounds per tier at 64x64)
            // 10% = 3 wounds, 50% = 7 wounds, 100% = 12 wounds
            int densityPercent = win.demistorm.visual_health.ConfigHelper.INSTANCE.woundDensityPercentage;
            int baseWoundsPerTier = 2 + (densityPercent * 10) / 100;

            // Scale by texture area so larger mobs get proportionally more wounds
            int woundsPerTier = (int) (baseWoundsPerTier * areaScale);
            int totalWounds = woundsPerTier * damageTier;

            // Use entity UUID for consistent random seed
            Random random = new Random(entity.getUUID().getLeastSignificantBits());

            // Calculate minimum distance between wounds for rejection sampling
            // Based on texture size to ensure even distribution at any scale
            int minDistance = (int) Math.sqrt(baseWidth * baseHeight) / 4;

            if (VisualHealth.debugMode) {
                VisualHealth.LOGGER.debug("Generating {}x{} wound texture (area scale: {:.2f}) with {} wounds for {}",
                        baseWidth, baseHeight, areaScale, totalWounds, entity.getName().getString());
            }

            // Track wound positions for rejection sampling (reset per tier)
            List<int[]> rejectedPositions = new ArrayList<>();

            // Stamp wound textures onto the base texture
            // Loop through each tier up to current tier to make wounds cumulative
            int woundIndex = 0;
            for (int tier = 1; tier <= damageTier; tier++) {
                // Clear position tracking for each tier (allow overlap between tiers)
                rejectedPositions.clear();

                // Get the damage type that caused this tier
                DamageType damageType = EntityHealthTracker.getDamageTypeForTier(entity.getId(), tier);

                // Get the appropriate tint for this damage type
                int woundTint = TintCalculator.getTintForDamageType(damageType, entity);

                if (VisualHealth.debugMode) {
                    VisualHealth.LOGGER.debug("Tier {}: damage type={}, tint=0x{}",
                            tier, damageType, Integer.toHexString(woundTint));
                }

                // Stamp wounds for this tier
                for (int i = 0; i < woundsPerTier; i++) {
                    try {
                        // Get a random wound texture for this damage type
                        Identifier woundAssetId = WoundAssetSelector.getRandomWoundTexture(damageType, random);

                        // Load the wound texture from resource manager
                        net.minecraft.server.packs.resources.ResourceManager resourceManager =
                                net.minecraft.client.Minecraft.getInstance().getResourceManager();

                        NativeImage woundAsset;
                        try (var resource = resourceManager.open(woundAssetId)) {
                            woundAsset = NativeImage.read(resource);
                        }

                        // CRITICAL: Apply tint to wound BEFORE stamping
                        // Each wound gets tinted individually based on its damage type
                        NativeImage tintedWound = TintUtils.applyTint(woundAsset, woundTint);

                        // Find position using rejection sampling for even distribution
                        int[] position = findValidPosition(baseWidth, baseHeight, tintedWound.getWidth(),
                                tintedWound.getHeight(), rejectedPositions, minDistance, random);

                        if (VisualHealth.debugMode) {
                            VisualHealth.LOGGER.debug("Stamping wound {} (tier {}, {}) at ({}, {})",
                                    ++woundIndex, tier, damageType, position[0], position[1]);
                        }

                        // Stamp the tinted wound onto the base texture
                        stampTexture(woundTexture, tintedWound, position[0], position[1]);

                        // Clean up tinted wound (important!)
                        tintedWound.close();
                        woundAsset.close();

                    } catch (Exception e) {
                        VisualHealth.LOGGER.error("Failed to load or stamp wound texture: {}", e.getMessage(), e);
                    }
                }
            }

            // Register the composite texture as a dynamic texture
            TextureManager textureManager = net.minecraft.client.Minecraft.getInstance().getTextureManager();
            Identifier dynamicTextureId = Identifier.fromNamespaceAndPath("visualhealth",
                    "dynamic/wounds/" + entity.getId() + "/tier" + damageTier);

            if (VisualHealth.debugMode) {
                VisualHealth.LOGGER.debug("Registering dynamic wound texture: {}", dynamicTextureId);
            }

            // Wrap the NativeImage in a DynamicTexture for GPU upload
            // Supplier provides the texture name for debugging
            DynamicTexture texture = new DynamicTexture(
                    dynamicTextureId::toString,
                    woundTexture
            );

            // Register the texture
            textureManager.register(dynamicTextureId, texture);

            // Cache the result
            WOUND_CACHE.put(cacheKey, dynamicTextureId);

            return dynamicTextureId;

        } catch (Exception e) {
            VisualHealth.LOGGER.error("Failed to generate wound texture: {}", e.getMessage(), e);
            return getFallbackTexture();
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
        // (better than failing, wounds will just be closer together)
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

        for (String key : WOUND_CACHE.keySet()) {
            if (key.startsWith(entityId + "_tier")) {
                // Extract tier number from key
                String tierStr = key.substring(key.lastIndexOf("tier") + 4);
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
            WOUND_CACHE.remove(key);
            cleared++;
        }

        if (cleared > 0 && VisualHealth.debugMode) {
            VisualHealth.LOGGER.debug("Cleared {} wound texture cache entries for entity ID {} (tiers {}-{})",
                    cleared, entityId, minTier, maxTier);
        }
    }

    // Stamp a small texture onto a base texture at the specified position
    // Handles alpha blending for smooth wound edges
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
                // If base is transparent, use stamp pixel directly
                if (baseAlpha == 0) {
                    baseTexture.setPixel(baseX, baseY, stampPixel);
                } else {
                    // Blend based on alpha
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
    }

    // Blend two color channels based on alpha ratio
    private static int blendChannel(int base, int stamp, float alphaRatio) {
        return (int)(base * (1.0f - alphaRatio) + stamp * alphaRatio);
    }

    // Fallback texture if generation fails
    private static Identifier getFallbackTexture() {
        return Identifier.fromNamespaceAndPath("visualhealth", "damage/scratches/scratch1.png");
    }
}
