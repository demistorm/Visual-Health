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

import java.util.*;

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

    // Cache texture data for debugging/saving
    // Key: entityId + damageTier
    // Value: NativeImage
    private static final Map<String, NativeImage> WOUND_IMAGE_CACHE = new HashMap<>();

    // Base reference texture size for wound count scaling
    private static final int BASE_TEXTURE_SIZE = 64;

    // Generate a texture with wounds stamped onto it
    // Returns a texture identifier that can be used with entity models
    public static Identifier generateWoundedTexture(LivingEntity entity, int damageTier, int baseWidth, int baseHeight) {
        // Build cache key that includes damage type history
        // This ensures different weapon combinations get different cached textures
        // Format: entityId_tier#_weapon1_weapon2_...
        StringBuilder cacheKeyBuilder = new StringBuilder();
        cacheKeyBuilder.append(entity.getId()).append("_tier").append(damageTier);

        // Include damage type for each tier in the cache key
        for (int tier = 1; tier <= damageTier; tier++) {
            DamageType damageType = EntityHealthTracker.getDamageTypeForTier(entity.getId(), tier);
            cacheKeyBuilder.append("_").append(damageType.name());
        }

        String cacheKey = cacheKeyBuilder.toString();
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

            // Base wound count from percentage (10-100% scales linearly 9-96 wounds per tier at 64x64)
            // 10% = 9 wounds per tier, 100% = 96 wounds per tier (384 wounds at tier 4)
            int densityPercent = win.demistorm.visual_health.ConfigHelper.INSTANCE.woundDensityPercentage;
            int baseWoundsPerTier = (densityPercent * 96) / 100;

            // Scale by texture area so larger mobs get proportionally more wounds
            int woundsPerTier = (int) (baseWoundsPerTier * areaScale);

            if (VisualHealth.debugMode) {
                VisualHealth.LOGGER.debug("Generating {}x{} wound texture (area scale: {:.2f}) with {} wounds per tier for {}",
                        baseWidth, baseHeight, areaScale, woundsPerTier, entity.getName().getString());
            }

            // Stamp wound textures onto the base texture
            // Loop through each tier up to current tier to make wounds cumulative
            int woundIndex = 0;
            for (int tier = 1; tier <= damageTier; tier++) {
                // Get the damage type that caused this tier
                DamageType damageType = EntityHealthTracker.getDamageTypeForTier(entity.getId(), tier);

                // Create a tier-specific random seed to ensure wounds stay in consistent positions
                // The seed includes the weapon sequence up to this tier, so tier 1-2 wounds
                // are in the same positions whether viewing tier 2 or tier 3
                long tierSeed = entity.getUUID().getLeastSignificantBits();
                for (int t = 1; t <= tier; t++) {
                    DamageType dt = EntityHealthTracker.getDamageTypeForTier(entity.getId(), t);
                    tierSeed = tierSeed * 31 + dt.name().hashCode();
                }
                Random tierRandom = new Random(tierSeed);

                // Pre-shuffle grid ONCE for this tier (ensures consistency)
                int[] tierCells = shuffleGrid(baseWidth, baseHeight, tierRandom);

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
                        Identifier woundAssetId = WoundAssetSelector.getRandomWoundTexture(damageType, tierRandom);

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

                        // Find position using fuzzy grid distribution for even coverage
                        int[] position = getFuzzyGridPosition(baseWidth, baseHeight,
                                tintedWound.getWidth(), tintedWound.getHeight(),
                                tierCells, i, tierRandom);

                        VisualHealth.LOGGER.info("Stamping wound at ({},{}) - wound size {}x{}",
                                position[0], position[1], tintedWound.getWidth(), tintedWound.getHeight());

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
            WOUND_IMAGE_CACHE.put(cacheKey, woundTexture);

            return dynamicTextureId;

        } catch (Exception e) {
            VisualHealth.LOGGER.error("Failed to generate wound texture: {}", e.getMessage(), e);
            return getFallbackTexture();
        }
    }

    // Shuffle grid cells into random order for consistent per-tier wound placement
    // Uses 8x8 pixel cells to ensure even coverage across entire texture including edges
    private static int[] shuffleGrid(int textureWidth, int textureHeight, Random random) {
        // Grid cells are always 8x8 pixels
        int gridCols = textureWidth / 8;
        int gridRows = textureHeight / 8;
        int totalCells = gridRows * gridCols;

        int[] cells = new int[totalCells];
        for (int i = 0; i < totalCells; i++) {
            cells[i] = i;
        }

        // Fisher-Yates shuffle
        for (int i = totalCells - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            int temp = cells[i];
            cells[i] = cells[j];
            cells[j] = temp;
        }

        return cells;
    }

    // Get fuzzy grid position for wound placement
    // Picks from pre-shuffled grid and adds random offset (-10 to +10 pixels) for natural look
    private static int[] getFuzzyGridPosition(int textureWidth, int textureHeight,
                                               int woundWidth, int woundHeight,
                                               int[] shuffledCells, int woundIndex,
                                               Random random) {
        // Grid cells are 8x8 pixels
        int gridCols = textureWidth / 8;
        int gridRows = textureHeight / 8;
        int cellWidth = 8;
        int cellHeight = 8;

        // Pick cell from shuffled array (wrap if more wounds than cells)
        int totalCells = gridRows * gridCols;
        int cellIndex = shuffledCells[woundIndex % totalCells];
        int cellRow = cellIndex / gridCols;
        int cellCol = cellIndex % gridCols;

        // Calculate cell center (always 8x8 grid, shifted +1 Y)
        int centerX = cellCol * cellWidth + cellWidth / 2;
        int centerY = cellRow * cellHeight + cellHeight / 2;

        // Add fuzziness (-8 to +8 pixels) for natural distribution
        int offsetX = random.nextInt(17) - 8;
        int offsetY = random.nextInt(17) - 8;

        // Calculate final position (wound centered at cell center)
        int x = centerX + offsetX - woundWidth / 2;
        int y = centerY + offsetY - woundHeight / 2;

        // Clamp to texture bounds (prevent negative or out-of-bounds placement)
        x = Math.max(0, Math.min(textureWidth - woundWidth, x));
        y = Math.max(0, Math.min(textureHeight - woundHeight, y));

        return new int[]{x, y};
    }

    // Save all currently cached wound textures to VHDamage directory
    public static void saveAllCachedTextures() {
        java.io.File outputDir = new java.io.File("VHDamage");
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        int savedCount = 0;
        for (Map.Entry<String, NativeImage> entry : WOUND_IMAGE_CACHE.entrySet()) {
            String cacheKey = entry.getKey();
            NativeImage image = entry.getValue();

            // Create safe filename from cache key
            String filename = cacheKey.replaceAll("[^a-zA-Z0-9_-]", "_") + ".png";
            java.io.File outputFile = new java.io.File(outputDir, filename);

            try {
                image.writeToFile(outputFile);
                savedCount++;

                if (VisualHealth.debugMode) {
                    VisualHealth.LOGGER.debug("Saved wound texture: {}", filename);
                }
            } catch (Exception e) {
                VisualHealth.LOGGER.error("Failed to save wound texture {}: {}", filename, e.getMessage());
            }
        }

        VisualHealth.LOGGER.info("Saved {} wound texture(s) to VHDamage directory", savedCount);
    }

    // Clear texture cache for specific entity tiers
    // Called when entity heals and loses damage tiers
    public static void clearEntityTiers(int entityId, int minTier, int maxTier) {
        int cleared = 0;
        List<String> keysToRemove = new ArrayList<>();

        for (String key : WOUND_CACHE.keySet()) {
            if (key.startsWith(entityId + "_tier")) {
                // Extract tier number from key (format: entityId_tier#_weapon1_weapon2_...)
                // Find the underscore after the tier number
                int tierEndIndex = key.indexOf("_", key.indexOf("tier") + 4);
                if (tierEndIndex == -1) {
                    tierEndIndex = key.length(); // No weapon sequence in key
                }

                String tierStr = key.substring(key.indexOf("tier") + 4, tierEndIndex);
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
            WOUND_IMAGE_CACHE.remove(key);
            cleared++;
        }

        if (cleared > 0 && VisualHealth.debugMode) {
            VisualHealth.LOGGER.debug("Cleared {} wound texture cache entries for entity ID {} (tiers {}-{})",
                    cleared, entityId, minTier, maxTier);
        }
    }

    // Clear all cached textures
    // Called on resource reload to invalidate all texture identifiers
    public static void clearAllCaches() {
        int cacheSize = WOUND_CACHE.size();
        WOUND_CACHE.clear();
        WOUND_IMAGE_CACHE.clear();

        if (cacheSize > 0) {
            VisualHealth.LOGGER.info("Cleared {} wound texture cache entries on resource reload", cacheSize);
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
