package win.demistorm.visual_health.client.texture;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import win.demistorm.visual_health.ConfigHelper;
import win.demistorm.visual_health.VisualHealth;
import win.demistorm.visual_health.client.entitymappings.DamageType;
import win.demistorm.visual_health.client.damagestate.EntityHealthTracker;
import win.demistorm.visual_health.client.damagestate.TintCalculator;
import win.demistorm.visual_health.client.renderer.WoundAssetSelector;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static win.demistorm.visual_health.client.texture.AlphaMaskCache.getOrGenerateAlphaMask;

public class WoundTextureGenerator {

    private WoundTextureGenerator() {
    }

    private static final Map<String, ResourceLocation> WOUND_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, NativeImage> WOUND_IMAGE_CACHE = new ConcurrentHashMap<>();

    private static final Map<String, ResourceLocation> WEAPON_WOUND_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, NativeImage> WEAPON_WOUND_IMAGE_CACHE = new ConcurrentHashMap<>();

    private static final Map<String, ResourceLocation> COMPOSITED_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, NativeImage> COMPOSITED_IMAGE_CACHE = new ConcurrentHashMap<>();

    private static final int BASE_TEXTURE_SIZE = 64;

    public static ResourceLocation generateCompositedTexture(
            LivingEntity entity, int damageTier, ResourceLocation baseTexture) {

        StringBuilder cacheKeyBuilder = new StringBuilder();
        cacheKeyBuilder.append("composite_").append(baseTexture.toString());
        cacheKeyBuilder.append("_e").append(entity.getId()).append("_tier").append(damageTier);

        for (int tier = 1; tier <= damageTier; tier++) {
            DamageType damageType = EntityHealthTracker.getDamageTypeForTier(entity.getId(), tier);
            cacheKeyBuilder.append("_").append(damageType.name());
        }

        String cacheKey = cacheKeyBuilder.toString();
        if (COMPOSITED_CACHE.containsKey(cacheKey)) {
            return COMPOSITED_CACHE.get(cacheKey);
        }

        try {
            NativeImage baseImage = SkinTextureReader.readTexture(baseTexture);
            if (baseImage == null) {
                VisualHealth.LOGGER.error("Failed to load base texture {} for compositing", baseTexture);
                return null;
            }

            int textureWidth = baseImage.getWidth();
            int textureHeight = baseImage.getHeight();

            NativeImage composited = new NativeImage(textureWidth, textureHeight, true);
            for (int y = 0; y < textureHeight; y++) {
                for (int x = 0; x < textureWidth; x++) {
                    composited.setPixelRGBA(x, y, baseImage.getPixelRGBA(x, y));
                }
            }

            boolean[][] alphaMask = getOrGenerateAlphaMask(baseTexture);

            double areaScale = (textureWidth * textureHeight) / (double) (BASE_TEXTURE_SIZE * BASE_TEXTURE_SIZE);
            int densityPercent = win.demistorm.visual_health.ConfigHelper.INSTANCE.woundDensityPercentage;
            int numTiers = ConfigHelper.INSTANCE.damageTierCount;
            int baseWoundsPerTier = (densityPercent * 115) / 100;
            int woundsPerTier = (int) (baseWoundsPerTier * areaScale * 5.0 / numTiers);

            VisualHealth.LOGGER.debug("Compositing {}x{} texture for {} (area scale: {}) with {} wounds per tier ({} total tiers)",
                    textureWidth, textureHeight, entity.getName().getString(), areaScale, woundsPerTier);

            int woundIndex = 0;
            net.minecraft.server.packs.resources.ResourceManager resourceManager =
                    net.minecraft.client.Minecraft.getInstance().getResourceManager();
            for (int tier = 1; tier <= damageTier; tier++) {
                DamageType damageType = EntityHealthTracker.getDamageTypeForTier(entity.getId(), tier);

                long tierSeed = entity.getUUID().getLeastSignificantBits();
                for (int t = 1; t <= tier; t++) {
                    DamageType dt = EntityHealthTracker.getDamageTypeForTier(entity.getId(), t);
                    tierSeed = tierSeed * 31 + dt.name().hashCode();
                }
                Random tierRandom = new Random(tierSeed);

                int[] tierCells = WoundTextureUtils.shuffleGrid(textureWidth, textureHeight, tierRandom);

                int woundTint = TintCalculator.getTintForDamageType(damageType, entity);

                for (int i = 0; i < woundsPerTier; i++) {
                    try {
                        ResourceLocation woundAssetId = WoundAssetSelector.getRandomWoundTexture(damageType, tierRandom);

                        NativeImage woundAsset;
                        try (var resource = resourceManager.open(woundAssetId)) {
                            woundAsset = NativeImage.read(resource);
                        }

                        NativeImage tintedWound = TintUtils.applyTint(woundAsset, woundTint);

                        int[] position = WoundTextureUtils.getFuzzyGridPosition(textureWidth, textureHeight,
                                tintedWound.getWidth(), tintedWound.getHeight(),
                                tierCells, i, tierRandom);

                        WoundTextureUtils.stampTexture(composited, tintedWound, position[0], position[1]);

                        VisualHealth.LOGGER.debug("Stamped wound {} (tier {}, {}) at ({}, {}) on composited texture",
                                ++woundIndex, tier, damageType, position[0], position[1]);

                        tintedWound.close();
                        woundAsset.close();

                    } catch (Exception e) {
                        VisualHealth.LOGGER.error("Failed to load or stamp wound texture: {}", e.getMessage(), e);
                    }
                }
            }

            if (alphaMask != null) {
                AlphaMaskCache.applyAlphaMaskToTexture(composited, alphaMask);
            }

            TextureManager textureManager = net.minecraft.client.Minecraft.getInstance().getTextureManager();
            ResourceLocation dynamicTextureId = new ResourceLocation("visualhealth",
                    "dynamic/composite/" + entity.getId() + "/" + baseTexture.getPath().replace('/', '_') + "_tier" + damageTier);

            DynamicTexture texture = new DynamicTexture(composited);
            textureManager.register(dynamicTextureId, texture);

            COMPOSITED_CACHE.put(cacheKey, dynamicTextureId);
            COMPOSITED_IMAGE_CACHE.put(cacheKey, composited);

            baseImage.close();

            return dynamicTextureId;

        } catch (Exception e) {
            VisualHealth.LOGGER.error("Failed to generate composited texture: {}", e.getMessage(), e);
            return null;
        }
    }

    public static ResourceLocation generateWoundedTexture(LivingEntity entity, int damageTier, int baseWidth, int baseHeight) {
        StringBuilder cacheKeyBuilder = new StringBuilder();
        cacheKeyBuilder.append(entity.getId()).append("_tier").append(damageTier);

        for (int tier = 1; tier <= damageTier; tier++) {
            DamageType damageType = EntityHealthTracker.getDamageTypeForTier(entity.getId(), tier);
            cacheKeyBuilder.append("_").append(damageType.name());
        }

        String cacheKey = cacheKeyBuilder.toString();
        if (WOUND_CACHE.containsKey(cacheKey)) {
            return WOUND_CACHE.get(cacheKey);
        }

        try {
            NativeImage woundTexture = new NativeImage(baseWidth, baseHeight, true);

            int transparent = 0x00000000;
            for (int y = 0; y < baseHeight; y++) {
                for (int x = 0; x < baseWidth; x++) {
                    woundTexture.setPixelRGBA(x, y, transparent);
                }
            }

            ResourceLocation entityTexture = TextureLocator.getEntityTexture(entity);
            boolean[][] alphaMask = null;

            if (entityTexture != null) {
                alphaMask = getOrGenerateAlphaMask(entityTexture);
            }

            double areaScale = (baseWidth * baseHeight) / (double) (BASE_TEXTURE_SIZE * BASE_TEXTURE_SIZE);
            int densityPercent = win.demistorm.visual_health.ConfigHelper.INSTANCE.woundDensityPercentage;
            int numTiers = ConfigHelper.INSTANCE.damageTierCount;
            int baseWoundsPerTier = (densityPercent * 115) / 100;
            int woundsPerTier = (int) (baseWoundsPerTier * areaScale * 5.0 / numTiers);

            int woundIndex = 0;
            for (int tier = 1; tier <= damageTier; tier++) {
                DamageType damageType = EntityHealthTracker.getDamageTypeForTier(entity.getId(), tier);

                long tierSeed = entity.getUUID().getLeastSignificantBits();
                for (int t = 1; t <= tier; t++) {
                    DamageType dt = EntityHealthTracker.getDamageTypeForTier(entity.getId(), t);
                    tierSeed = tierSeed * 31 + dt.name().hashCode();
                }
                Random tierRandom = new Random(tierSeed);

                int[] tierCells = WoundTextureUtils.shuffleGrid(baseWidth, baseHeight, tierRandom);

                int woundTint = TintCalculator.getTintForDamageType(damageType, entity);

                for (int i = 0; i < woundsPerTier; i++) {
                    try {
                        ResourceLocation woundAssetId = WoundAssetSelector.getRandomWoundTexture(damageType, tierRandom);

                        net.minecraft.server.packs.resources.ResourceManager resourceManager =
                                net.minecraft.client.Minecraft.getInstance().getResourceManager();

                        NativeImage woundAsset;
                        try (var resource = resourceManager.open(woundAssetId)) {
                            woundAsset = NativeImage.read(resource);
                        }

                        NativeImage tintedWound = TintUtils.applyTint(woundAsset, woundTint);

                        int[] position = WoundTextureUtils.getFuzzyGridPosition(baseWidth, baseHeight,
                                tintedWound.getWidth(), tintedWound.getHeight(),
                                tierCells, i, tierRandom);

                        WoundTextureUtils.stampTexture(woundTexture, tintedWound, position[0], position[1]);

                        tintedWound.close();
                        woundAsset.close();

                    } catch (Exception e) {
                        VisualHealth.LOGGER.error("Failed to load or stamp wound texture: {}", e.getMessage(), e);
                    }
                }
            }

            if (alphaMask != null) {
                AlphaMaskCache.applyAlphaMaskToTexture(woundTexture, alphaMask);
            }

            TextureManager textureManager = net.minecraft.client.Minecraft.getInstance().getTextureManager();
            ResourceLocation dynamicTextureId = new ResourceLocation("visualhealth",
                    "dynamic/wounds/" + entity.getId() + "/tier" + damageTier);

            DynamicTexture texture = new DynamicTexture(woundTexture);
            textureManager.register(dynamicTextureId, texture);

            WOUND_CACHE.put(cacheKey, dynamicTextureId);
            WOUND_IMAGE_CACHE.put(cacheKey, woundTexture);

            return dynamicTextureId;

        } catch (Exception e) {
            VisualHealth.LOGGER.error("Failed to generate wound texture: {}", e.getMessage(), e);
            return getFallbackTexture();
        }
    }

    public static boolean hasWeaponTiers(int entityId, int damageTier) {
        for (int tier = 1; tier <= damageTier; tier++) {
            if (EntityHealthTracker.getDamageTypeForTier(entityId, tier) != DamageType.GENERIC) {
                return true;
            }
        }
        return false;
    }

    public static ResourceLocation generateWeaponOnlyTexture(LivingEntity entity, int damageTier, int baseWidth, int baseHeight) {
        StringBuilder cacheKeyBuilder = new StringBuilder();
        cacheKeyBuilder.append("weapons_").append(entity.getId()).append("_tier").append(damageTier);

        for (int tier = 1; tier <= damageTier; tier++) {
            DamageType damageType = EntityHealthTracker.getDamageTypeForTier(entity.getId(), tier);
            cacheKeyBuilder.append("_").append(damageType.name());
        }

        String cacheKey = cacheKeyBuilder.toString();
        if (WEAPON_WOUND_CACHE.containsKey(cacheKey)) {
            return WEAPON_WOUND_CACHE.get(cacheKey);
        }

        try {
            NativeImage woundTexture = new NativeImage(baseWidth, baseHeight, true);

            int transparent = 0x00000000;
            for (int y = 0; y < baseHeight; y++) {
                for (int x = 0; x < baseWidth; x++) {
                    woundTexture.setPixelRGBA(x, y, transparent);
                }
            }

            ResourceLocation entityTexture = TextureLocator.getEntityTexture(entity);
            boolean[][] alphaMask = null;

            if (entityTexture != null) {
                alphaMask = getOrGenerateAlphaMask(entityTexture);
            }

            double areaScale = (baseWidth * baseHeight) / (double) (BASE_TEXTURE_SIZE * BASE_TEXTURE_SIZE);
            int densityPercent = win.demistorm.visual_health.ConfigHelper.INSTANCE.woundDensityPercentage;
            int numTiers = ConfigHelper.INSTANCE.damageTierCount;
            int baseWoundsPerTier = (densityPercent * 115) / 100;
            int woundsPerTier = (int) (baseWoundsPerTier * areaScale * 5.0 / numTiers);

            for (int tier = 1; tier <= damageTier; tier++) {
                DamageType damageType = EntityHealthTracker.getDamageTypeForTier(entity.getId(), tier);
                if (damageType == DamageType.GENERIC) {
                    continue;
                }

                long tierSeed = entity.getUUID().getLeastSignificantBits();
                for (int t = 1; t <= tier; t++) {
                    DamageType dt = EntityHealthTracker.getDamageTypeForTier(entity.getId(), t);
                    tierSeed = tierSeed * 31 + dt.name().hashCode();
                }
                Random tierRandom = new Random(tierSeed);

                int[] tierCells = WoundTextureUtils.shuffleGrid(baseWidth, baseHeight, tierRandom);

                int woundTint = TintCalculator.getTintForDamageType(damageType, entity);

                for (int i = 0; i < woundsPerTier; i++) {
                    try {
                        ResourceLocation woundAssetId = WoundAssetSelector.getRandomWoundTexture(damageType, tierRandom);

                        net.minecraft.server.packs.resources.ResourceManager resourceManager =
                                net.minecraft.client.Minecraft.getInstance().getResourceManager();

                        NativeImage woundAsset;
                        try (var resource = resourceManager.open(woundAssetId)) {
                            woundAsset = NativeImage.read(resource);
                        }

                        NativeImage tintedWound = TintUtils.applyTint(woundAsset, woundTint);

                        int[] position = WoundTextureUtils.getFuzzyGridPosition(baseWidth, baseHeight,
                                tintedWound.getWidth(), tintedWound.getHeight(),
                                tierCells, i, tierRandom);

                        WoundTextureUtils.stampTexture(woundTexture, tintedWound, position[0], position[1]);

                        tintedWound.close();
                        woundAsset.close();

                    } catch (Exception e) {
                        VisualHealth.LOGGER.error("Failed to load or stamp weapon wound texture: {}", e.getMessage(), e);
                    }
                }
            }

            if (alphaMask != null) {
                AlphaMaskCache.applyAlphaMaskToTexture(woundTexture, alphaMask);
            }

            TextureManager textureManager = net.minecraft.client.Minecraft.getInstance().getTextureManager();
            ResourceLocation dynamicTextureId = new ResourceLocation("visualhealth",
                    "dynamic/weapons/" + entity.getId() + "/tier" + damageTier);

            DynamicTexture texture = new DynamicTexture(woundTexture);
            textureManager.register(dynamicTextureId, texture);

            WEAPON_WOUND_CACHE.put(cacheKey, dynamicTextureId);
            WEAPON_WOUND_IMAGE_CACHE.put(cacheKey, woundTexture);

            return dynamicTextureId;

        } catch (Exception e) {
            VisualHealth.LOGGER.error("Failed to generate weapon-only wound texture: {}", e.getMessage(), e);
            return getFallbackTexture();
        }
    }

    public static void saveAllCachedTextures() {
        java.io.File outputDir = new java.io.File("VHDamage");
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        int savedCount = 0;
        for (Map.Entry<String, NativeImage> entry : WOUND_IMAGE_CACHE.entrySet()) {
            String cacheKey = entry.getKey();
            NativeImage image = entry.getValue();

            String filename = cacheKey.replaceAll("[^a-zA-Z0-9_-]", "_") + ".png";
            java.io.File outputFile = new java.io.File(outputDir, filename);

            try {
                image.writeToFile(outputFile);
                savedCount++;
            } catch (Exception e) {
                VisualHealth.LOGGER.error("Failed to save wound texture {}: {}", filename, e.getMessage());
            }
        }

        for (Map.Entry<String, NativeImage> entry : WEAPON_WOUND_IMAGE_CACHE.entrySet()) {
            String cacheKey = entry.getKey();
            NativeImage image = entry.getValue();

            String filename = cacheKey.replaceAll("[^a-zA-Z0-9_-]", "_") + ".png";
            java.io.File outputFile = new java.io.File(outputDir, filename);

            try {
                image.writeToFile(outputFile);
                savedCount++;
            } catch (Exception e) {
                VisualHealth.LOGGER.error("Failed to save weapon wound texture {}: {}", filename, e.getMessage());
            }
        }

        for (Map.Entry<String, NativeImage> entry : COMPOSITED_IMAGE_CACHE.entrySet()) {
            String cacheKey = entry.getKey();
            NativeImage image = entry.getValue();

            String filename = cacheKey.replaceAll("[^a-zA-Z0-9_-]", "_") + ".png";
            java.io.File outputFile = new java.io.File(outputDir, filename);

            try {
                image.writeToFile(outputFile);
                savedCount++;
            } catch (Exception e) {
                VisualHealth.LOGGER.error("Failed to save composited texture {}: {}", filename, e.getMessage());
            }
        }

        VisualHealth.LOGGER.info("Saved {} texture(s) to VHDamage directory", savedCount);
    }

    public static void clearEntityTiers(int entityId, int minTier, int maxTier) {
        int cleared = 0;
        List<String> keysToRemove = new ArrayList<>();

        for (String key : WOUND_CACHE.keySet()) {
            if (key.startsWith(entityId + "_tier")) {
                int tierEndIndex = key.indexOf("_", key.indexOf("tier") + 4);
                if (tierEndIndex == -1) {
                    tierEndIndex = key.length();
                }

                String tierStr = key.substring(key.indexOf("tier") + 4, tierEndIndex);
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
            WOUND_CACHE.remove(key);
            WOUND_IMAGE_CACHE.remove(key);
            cleared++;
        }

        keysToRemove.clear();

        for (String key : WEAPON_WOUND_CACHE.keySet()) {
            if (key.startsWith("weapons_" + entityId + "_tier")) {
                int tierStart = key.indexOf("_tier");
                int tierEndIndex = key.indexOf("_", tierStart + 5);
                if (tierEndIndex == -1) {
                    tierEndIndex = key.length();
                }

                String tierStr = key.substring(tierStart + 5, tierEndIndex);
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
            WEAPON_WOUND_CACHE.remove(key);
            WEAPON_WOUND_IMAGE_CACHE.remove(key);
            cleared++;
        }

        keysToRemove.clear();

        for (String key : COMPOSITED_CACHE.keySet()) {
            if (key.contains("_e" + entityId + "_tier")) {
                int tierStart = key.indexOf("_tier", key.indexOf("_e" + entityId));
                if (tierStart == -1) continue;

                int tierEndIndex = key.indexOf("_", tierStart + 5);
                if (tierEndIndex == -1) {
                    tierEndIndex = key.length();
                }

                String tierStr = key.substring(tierStart + 5, tierEndIndex);
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
            COMPOSITED_CACHE.remove(key);
            COMPOSITED_IMAGE_CACHE.remove(key);
            cleared++;
        }

        if (cleared > 0) {
            VisualHealth.LOGGER.debug("Cleared {} texture cache entries for entity ID {} (tiers {}-{})",
                    cleared, entityId, minTier, maxTier);
        }
    }

    public static void clearAllCaches() {
        int cacheSize = WOUND_CACHE.size() + WEAPON_WOUND_CACHE.size() + COMPOSITED_CACHE.size();

        for (NativeImage image : WOUND_IMAGE_CACHE.values()) {
            try { image.close(); } catch (Exception e) { }
        }
        for (NativeImage image : WEAPON_WOUND_IMAGE_CACHE.values()) {
            try { image.close(); } catch (Exception e) { }
        }
        for (NativeImage image : COMPOSITED_IMAGE_CACHE.values()) {
            try { image.close(); } catch (Exception e) { }
        }

        WOUND_CACHE.clear();
        WOUND_IMAGE_CACHE.clear();
        WEAPON_WOUND_CACHE.clear();
        WEAPON_WOUND_IMAGE_CACHE.clear();
        COMPOSITED_CACHE.clear();
        COMPOSITED_IMAGE_CACHE.clear();

        AlphaMaskCache.clearAllCaches();

        if (cacheSize > 0) {
            VisualHealth.LOGGER.info("Cleared {} texture cache entries on resource reload", cacheSize);
        }
    }

    public static void clearTextureCaches() {
        int cacheSize = WOUND_CACHE.size() + WEAPON_WOUND_CACHE.size() + COMPOSITED_CACHE.size();
        WOUND_CACHE.clear();
        WOUND_IMAGE_CACHE.clear();
        WEAPON_WOUND_CACHE.clear();
        WEAPON_WOUND_IMAGE_CACHE.clear();
        COMPOSITED_CACHE.clear();
        COMPOSITED_IMAGE_CACHE.clear();

        if (cacheSize > 0) {
            VisualHealth.LOGGER.info("Cleared {} texture cache entries on config change", cacheSize);
        }
    }

    public static Integer getCompositedTextureGLId(LivingEntity entity) {
        int tier = EntityHealthTracker.getDamageTier(entity.getId());
        if (tier <= 0) return null;

        ResourceLocation baseTexture = TextureLocator.getEntityTexture(entity);
        if (baseTexture == null) return null;

        StringBuilder keyBuilder = new StringBuilder();
        keyBuilder.append("composite_").append(baseTexture.toString());
        keyBuilder.append("_e").append(entity.getId()).append("_tier").append(tier);
        for (int t = 1; t <= tier; t++) {
            DamageType dt = EntityHealthTracker.getDamageTypeForTier(entity.getId(), t);
            keyBuilder.append("_").append(dt.name());
        }

        ResourceLocation composited = COMPOSITED_CACHE.get(keyBuilder.toString());
        if (composited == null) return null;

        try {
            net.minecraft.client.renderer.texture.AbstractTexture tex =
                    net.minecraft.client.Minecraft.getInstance().getTextureManager().getTexture(composited);
            if (tex != null) return tex.getId();
        } catch (Exception e) {
            VisualHealth.LOGGER.debug("Failed to get GL ID for composited texture: {}", e.getMessage());
        }
        return null;
    }

    private static ResourceLocation getFallbackTexture() {
        return new ResourceLocation("visualhealth", "damage/scratches/scratch1.png");
    }
}
