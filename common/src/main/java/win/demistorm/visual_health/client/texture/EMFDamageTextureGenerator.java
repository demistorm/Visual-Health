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

// Generate damaged EMF variant textures
public class EMFDamageTextureGenerator {

    private EMFDamageTextureGenerator() {
    }

    private static final Map<String, Identifier> DAMAGE_CACHE = new HashMap<>();

    private static final int BASE_TEXTURE_SIZE = 64;
    public static Identifier generateDamagedVariant(
            Identifier variantTexture,
            LivingEntity entity,
            int damageTier
    ) {
            StringBuilder cacheKeyBuilder = new StringBuilder();
            cacheKeyBuilder.append(variantTexture.toString()).append("_tier").append(damageTier).append("_entity").append(entity.getId());

            for (int tier = 1; tier <= damageTier; tier++) {
                DamageType damageType = EntityHealthTracker.getDamageTypeForTier(entity.getId(), tier);
                cacheKeyBuilder.append("_").append(damageType.name());
            }

            String cacheKey = cacheKeyBuilder.toString();

            if (DAMAGE_CACHE.containsKey(cacheKey)) {
                return DAMAGE_CACHE.get(cacheKey);
            }

        try {
            VisualHealth.LOGGER.debug("Generating EMF damaged variant: {} (tier {})",
                    variantTexture, damageTier);

            // Load the variant texture
            net.minecraft.server.packs.resources.ResourceManager resourceManager =
                    net.minecraft.client.Minecraft.getInstance().getResourceManager();

                if (variantTexture.getNamespace().equals("visualhealth") &&
                        variantTexture.getPath().startsWith("dynamic/emf_damage/")) {
                    return variantTexture;
                }

                NativeImage variantImage;
                try (var resource = resourceManager.open(variantTexture)) {
                    variantImage = NativeImage.read(resource);
                }

                NativeImage damagedVariant = new NativeImage(variantImage.getWidth(), variantImage.getHeight(), true);
                for (int y = 0; y < variantImage.getHeight(); y++) {
                    for (int x = 0; x < variantImage.getWidth(); x++) {
                        damagedVariant.setPixel(x, y, variantImage.getPixel(x, y));
                    }
                }

                double areaScale = (variantImage.getWidth() * variantImage.getHeight()) /
                        (double) (BASE_TEXTURE_SIZE * BASE_TEXTURE_SIZE);

                int densityPercent = win.demistorm.visual_health.ConfigHelper.INSTANCE.woundDensityPercentage;
                int baseWoundsPerTier = (densityPercent * 96) / 100;

                int woundsPerTier = (int) (baseWoundsPerTier * areaScale);

                int woundIndex = 0;
                for (int tier = 1; tier <= damageTier; tier++) {

                    DamageType damageType = EntityHealthTracker.getDamageTypeForTier(entity.getId(), tier);

                    long tierSeed = entity.getId();
                    for (int t = 1; t <= tier; t++) {
                        DamageType dt = EntityHealthTracker.getDamageTypeForTier(entity.getId(), t);
                        tierSeed = tierSeed * 31 + dt.name().hashCode();
                    }
                    Random tierRandom = new Random(tierSeed);

                    int[] tierCells = shuffleGrid(variantImage.getWidth(), variantImage.getHeight(), tierRandom);

                    int woundTint = TintCalculator.getTintForDamageType(damageType, entity);

                    for (int i = 0; i < woundsPerTier; i++) {
                        try {
                            Identifier woundAssetId = WoundAssetSelector.getRandomWoundTexture(damageType, tierRandom);

                            NativeImage woundAsset;
                            try (var resource = resourceManager.open(woundAssetId)) {
                                woundAsset = NativeImage.read(resource);
                            }

                            NativeImage tintedWound = TintUtils.applyTint(woundAsset, woundTint);

                            int[] position = getFuzzyGridPosition(variantImage.getWidth(), variantImage.getHeight(),
                                    tintedWound.getWidth(), tintedWound.getHeight(),
                                    tierCells, i, tierRandom);

                            VisualHealth.LOGGER.debug("Stamping wound {} (tier {}, {}) at ({}, {})",
                                    ++woundIndex, tier, damageType, position[0], position[1]);

                        // Stamp the tinted wound onto the variant texture
                        stampTexture(damagedVariant, tintedWound, position[0], position[1]);

                            tintedWound.close();
                            woundAsset.close();

                        } catch (Exception e) {
                            VisualHealth.LOGGER.error("Failed to load or stamp wound texture: {}", e.getMessage(), e);
                        }
                    }
                }

                TextureManager textureManager = net.minecraft.client.Minecraft.getInstance().getTextureManager();
                Identifier dynamicTextureId = Identifier.fromNamespaceAndPath("visualhealth",
                        "dynamic/emf_damage/" + entity.getId() + "/" + variantTexture.getPath().replace('/', '_') + "_tier" + damageTier);

                DynamicTexture texture = new DynamicTexture(
                        dynamicTextureId::toString,
                        damagedVariant
                );

                textureManager.register(dynamicTextureId, texture);

                DAMAGE_CACHE.put(cacheKey, dynamicTextureId);

                variantImage.close();

                return dynamicTextureId;

            } catch (Exception e) {
                VisualHealth.LOGGER.error("Failed to generate EMF damaged variant: {}", e.getMessage(), e);
                return variantTexture;
            }
    }

    private static int[] shuffleGrid(int textureWidth, int textureHeight, Random random) {
        int gridCols = textureWidth / 8;
        int gridRows = textureHeight / 8;
        int totalCells = gridRows * gridCols;

        int[] cells = new int[totalCells];
        for (int i = 0; i < totalCells; i++) {
            cells[i] = i;
        }

        for (int i = totalCells - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            int temp = cells[i];
            cells[i] = cells[j];
            cells[j] = temp;
        }

        return cells;
    }

    private static int[] getFuzzyGridPosition(int textureWidth, int textureHeight,
                                               int woundWidth, int woundHeight,
                                               int[] shuffledCells, int woundIndex,
                                               Random random) {
        int gridCols = textureWidth / 8;
        int gridRows = textureHeight / 8;
        int cellWidth = 8;
        int cellHeight = 8;

        int totalCells = gridRows * gridCols;
        int cellIndex = shuffledCells[woundIndex % totalCells];
        int cellRow = cellIndex / gridCols;
        int cellCol = cellIndex % gridCols;

        int centerX = cellCol * cellWidth + cellWidth / 2;
        int centerY = cellRow * cellHeight + cellHeight / 2;

        int offsetX = random.nextInt(17) - 8;
        int offsetY = random.nextInt(17) - 8;

        int x = centerX + offsetX - woundWidth / 2;
        int y = centerY + offsetY - woundHeight / 2;

        x = Math.max(0, Math.min(textureWidth - woundWidth, x));
        y = Math.max(0, Math.min(textureHeight - woundHeight, y));

        return new int[]{x, y};
    }

    public static void clearEntityTiers(int entityId, int minTier, int maxTier) {
        int cleared = 0;
        List<String> keysToRemove = new ArrayList<>();

        for (String key : DAMAGE_CACHE.keySet()) {
            if (key.contains("_entity" + entityId + "_")) {
                int tierStartIndex = key.indexOf("_tier");
                if (tierStartIndex == -1) {
                    continue;
                }

                int tierEndIndex = key.indexOf("_entity", tierStartIndex);
                if (tierEndIndex == -1) {
                    continue;
                }

                String tierStr = key.substring(tierStartIndex + 5, tierEndIndex);
                try {
                    int tier = Integer.parseInt(tierStr);
                    if (tier >= minTier && tier <= maxTier) {
                        keysToRemove.add(key);
                    }
                } catch (NumberFormatException e) {
                }
            }
        }

        for (String key : keysToRemove) {
            DAMAGE_CACHE.remove(key);
            cleared++;
        }

        if (cleared > 0) {
            VisualHealth.LOGGER.debug("Cleared {} EMF damage texture cache entries for entity ID {} (tiers {}-{})",
                    cleared, entityId, minTier, maxTier);
        }
    }

    public static void clearAllCaches() {
        int cacheSize = DAMAGE_CACHE.size();
        DAMAGE_CACHE.clear();

        if (cacheSize > 0) {
            VisualHealth.LOGGER.info("Cleared {} EMF damage texture cache entries on resource reload", cacheSize);
        }
    }

    public static void clearTextureCaches() {
        int cacheSize = DAMAGE_CACHE.size();
        DAMAGE_CACHE.clear();

        if (cacheSize > 0) {
            VisualHealth.LOGGER.info("Cleared {} EMF damage texture cache entries on config change", cacheSize);
        }
    }
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

    // Blend two colors based on alpha
    private static int blendChannel(int base, int stamp, float alphaRatio) {
        return (int)(base * (1.0f - alphaRatio) + stamp * alphaRatio);
    }

}
