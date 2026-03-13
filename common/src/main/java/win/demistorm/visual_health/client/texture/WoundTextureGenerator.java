package win.demistorm.visual_health.client.texture;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import win.demistorm.visual_health.VisualHealth;
import win.demistorm.visual_health.client.entitymappings.DamageType;
import win.demistorm.visual_health.client.damagestate.EntityHealthTracker;
import win.demistorm.visual_health.client.damagestate.TintCalculator;
import win.demistorm.visual_health.client.renderer.WoundAssetSelector;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static win.demistorm.visual_health.client.texture.AlphaMaskCache.getOrGenerateAlphaMask;
import static win.demistorm.visual_health.client.texture.TextureLocator.getEntityTexture;

// Generates composite textures with wound effects stamped onto them
// Creates entity-sized textures with wound overlays for the RenderLayer system
public class WoundTextureGenerator {

    private WoundTextureGenerator() {
        // Utility class - no instances
    }

    // Cache generated wounded textures
    // Key: entityId + damageTier
    // Value: Dynamic texture identifier
    private static final Map<String, Identifier> WOUND_CACHE = new ConcurrentHashMap<>();

    // Cache texture data for debugging/saving
    // Key: entityId + damageTier
    // Value: NativeImage
    private static final Map<String, NativeImage> WOUND_IMAGE_CACHE = new ConcurrentHashMap<>();

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

            // Get the entity's actual texture (including variants) for alpha masking
            Identifier entityTexture = getEntityTexture(entity);
            boolean[][] alphaMask = null;

            if (entityTexture != null) {
                alphaMask = getOrGenerateAlphaMask(entityTexture);
                VisualHealth.LOGGER.debug("Got alpha mask for texture {}: {}",
                        entityTexture, alphaMask != null ? "success" : "null");
            }

            // Calculate wound count based on texture area (scaling) and density percentage
            double areaScale = (baseWidth * baseHeight) / (double) (BASE_TEXTURE_SIZE * BASE_TEXTURE_SIZE);

            // Base wound count from percentage (10-100% scales linearly 9-96 wounds per tier at 64x64)
            // 10% = 9 wounds per tier, 100% = 96 wounds per tier (384 wounds at tier 4)
            int densityPercent = win.demistorm.visual_health.ConfigHelper.INSTANCE.woundDensityPercentage;
            int baseWoundsPerTier = (densityPercent * 96) / 100;

            // Scale by texture area so larger mobs get proportionally more wounds
            int woundsPerTier = (int) (baseWoundsPerTier * areaScale);

            VisualHealth.LOGGER.debug("Generating {}x{} wound texture (area scale: {}) with {} wounds per tier for {}",
                    baseWidth, baseHeight, areaScale, woundsPerTier, entity.getName().getString());

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
                int[] tierCells = WoundTextureUtils.shuffleGrid(baseWidth, baseHeight, tierRandom);

                // Get the appropriate tint for this damage type
                int woundTint = TintCalculator.getTintForDamageType(damageType, entity);

                VisualHealth.LOGGER.debug("Tier {}: damage type={}, tint=0x{}",
                        tier, damageType, Integer.toHexString(woundTint));

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
                        int[] position = WoundTextureUtils.getFuzzyGridPosition(baseWidth, baseHeight,
                                tintedWound.getWidth(), tintedWound.getHeight(),
                                tierCells, i, tierRandom);

                        VisualHealth.LOGGER.debug("Stamping wound at ({},{}) - wound size {}x{}",
                                position[0], position[1], tintedWound.getWidth(), tintedWound.getHeight());

                        VisualHealth.LOGGER.debug("Stamping wound {} (tier {}, {}) at ({}, {})",
                                ++woundIndex, tier, damageType, position[0], position[1]);

                        // Stamp the tinted wound onto the base texture
                        WoundTextureUtils.stampTexture(woundTexture, tintedWound, position[0], position[1]);

                        // Clean up tinted wound (important!)
                        tintedWound.close();
                        woundAsset.close();

                    } catch (Exception e) {
                        VisualHealth.LOGGER.error("Failed to load or stamp wound texture: {}", e.getMessage(), e);
                    }
                }
            }

            // Apply alpha mask to entire texture after all wounds are stamped
            // This prevents floating wounds on invisible model parts while allowing partial wounds
            if (alphaMask != null) {
                win.demistorm.visual_health.client.texture.AlphaMaskCache.applyAlphaMaskToTexture(woundTexture, alphaMask);
            }

            // Register the composite texture as a dynamic texture
            TextureManager textureManager = net.minecraft.client.Minecraft.getInstance().getTextureManager();
            Identifier dynamicTextureId = Identifier.fromNamespaceAndPath("visualhealth",
                    "dynamic/wounds/" + entity.getId() + "/tier" + damageTier);

            VisualHealth.LOGGER.debug("Registering dynamic wound texture: {}", dynamicTextureId);

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

                VisualHealth.LOGGER.debug("Saved wound texture: {}", filename);
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

        if (cleared > 0) {
            VisualHealth.LOGGER.debug("Cleared {} wound texture cache entries for entity ID {} (tiers {}-{})",
                    cleared, entityId, minTier, maxTier);
        }
    }

    // Clear all cached textures
    // Called on resource reload to invalidate all texture identifiers
    public static void clearAllCaches() {
        int cacheSize = WOUND_CACHE.size();

        // Close all NativeImage objects before clearing maps
        for (NativeImage image : WOUND_IMAGE_CACHE.values()) {
            try {
                image.close();
            } catch (Exception e) {
                VisualHealth.LOGGER.error("Failed to close NativeImage", e);
            }
        }

        WOUND_CACHE.clear();
        WOUND_IMAGE_CACHE.clear();

        // Clear alpha mask cache as well
        win.demistorm.visual_health.client.texture.AlphaMaskCache.clearAllCaches();

        if (cacheSize > 0) {
            VisualHealth.LOGGER.info("Cleared {} wound texture cache entries on resource reload", cacheSize);
        }
    }

    // Clear only texture caches (preserve alpha masks)
    // Called when config changes to regenerate textures with new settings
    public static void clearTextureCaches() {
        int cacheSize = WOUND_CACHE.size();
        WOUND_CACHE.clear();
        WOUND_IMAGE_CACHE.clear();

        if (cacheSize > 0) {
            VisualHealth.LOGGER.info("Cleared {} wound texture cache entries on config change", cacheSize);
        }
    }

    // Fallback texture if generation fails
    private static Identifier getFallbackTexture() {
        return Identifier.fromNamespaceAndPath("visualhealth", "damage/scratches/scratch1.png");
    }
}
