package win.demistorm.visual_health.client.texture;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import win.demistorm.visual_health.ConfigHelper;
import win.demistorm.visual_health.VisualHealth;
import win.demistorm.visual_health.client.entitymappings.DamageType;
import win.demistorm.visual_health.client.damagestate.EntityHealthTracker;
import win.demistorm.visual_health.client.damagestate.TintCalculator;
import win.demistorm.visual_health.client.renderer.WoundAssetSelector;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.ToIntFunction;
import java.util.function.Predicate;

import static win.demistorm.visual_health.client.texture.AlphaMaskCache.getOrGenerateAlphaMask;

public class WoundTextureGenerator {

    private WoundTextureGenerator() {}

    private static final Map<String, ResourceLocation> TEXTURE_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, NativeImage> IMAGE_CACHE = new ConcurrentHashMap<>();

    private static NativeImage copyNativeImage(NativeImage source) {
        int w = source.getWidth();
        int h = source.getHeight();
        NativeImage copy = new NativeImage(w, h, true);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                copy.setPixelRGBA(x, y, source.getPixelRGBA(x, y));
            }
        }
        return copy;
    }

    private static final int BASE_TEXTURE_SIZE = 64;
    private static final float GENERIC_WOUND_MIN_OPACITY = 0.30f;
    private static final float GENERIC_WOUND_MAX_OPACITY = 0.50f;
    private static final float WEAPON_WOUND_MIN_OPACITY = 0.70f;
    private static final float WEAPON_WOUND_MAX_OPACITY = 1.00f;

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String category;
        private LivingEntity entity;
        private String ownerId;
        private int damageTier;
        private ResourceLocation texture;
        private DamageType[] damageTypes;
        private Long seed;
        private ToIntFunction<DamageType> tintProvider;
        private Boolean transparent;
        private Predicate<DamageType> stampFilter;
        private float densityMultiplier = 1.0f;

        private Builder() {}

        public Builder category(String category) { this.category = category; return this; }
        public Builder entity(LivingEntity entity) { this.entity = entity; return this; }
        public Builder ownerId(String ownerId) { this.ownerId = ownerId; return this; }
        public Builder damageTier(int damageTier) { this.damageTier = damageTier; return this; }
        public Builder texture(ResourceLocation texture) { this.texture = texture; return this; }
        public Builder damageTypes(DamageType[] damageTypes) { this.damageTypes = damageTypes; return this; }
        public Builder seed(long seed) { this.seed = seed; return this; }
        public Builder tint(ToIntFunction<DamageType> tintProvider) { this.tintProvider = tintProvider; return this; }
        public Builder composite() {
            transparent = false;
            return this;
        }
        public Builder transparent() {
            transparent = true;
            return this;
        }
        public Builder stampFilter(Predicate<DamageType> filter) { this.stampFilter = filter; return this; }
        public Builder densityMultiplier(float densityMultiplier) { this.densityMultiplier = densityMultiplier; return this; }

        public ResourceLocation generate() {
            resolveDefaults();

            String cacheKey = WoundTextureGenerator.buildCacheKey(
                    category, ownerId, texture, damageTier, damageTypes, transparent);

            ResourceLocation cached = TEXTURE_CACHE.get(cacheKey);
            if (cached != null) return cached;

            try {
                String dynamicPath = WoundTextureGenerator.buildDynamicPath(
                        category, ownerId, texture, damageTier, transparent);

                if (transparent) {
                    return generateOverlay(cacheKey, dynamicPath);
                } else {
                    return generateComposited(cacheKey, dynamicPath);
                }
            } catch (Exception e) {
                VisualHealth.LOGGER.error("Failed to generate {} texture: {}",
                        transparent ? "overlay" : "composited", e.getMessage(), e);
                return null;
            }
        }

        private void resolveDefaults() {
            if (entity != null) {
                if (ownerId == null) ownerId = "e" + entity.getId();
                if (damageTypes == null)
                    damageTypes = WoundTextureGenerator.buildDamageTypesFromTracker(entity.getId(), damageTier);
                if (seed == null) seed = entity.getUUID().getLeastSignificantBits();
                if (tintProvider == null)
                    tintProvider = type -> TintCalculator.getTintForDamageType(type, entity);
            }
            if (stampFilter == null) stampFilter = type -> true;
            if (seed == null) seed = 0L;
            if (transparent == null) transparent = false;
        }

        @Nullable
        private NativeImage findPreviousTierCopy(int w, int h) {
            if (damageTier <= 1 || damageTypes.length <= 1) return null;

            DamageType[] prevTypes = Arrays.copyOf(damageTypes, damageTypes.length - 1);
            String prevKey = WoundTextureGenerator.buildCacheKey(
                    category, ownerId, texture, damageTier - 1, prevTypes, transparent);

            NativeImage prevImage = IMAGE_CACHE.get(prevKey);
            if (prevImage == null || prevImage.getWidth() != w || prevImage.getHeight() != h) {
                return null;
            }

            VisualHealth.LOGGER.debug("Incremental tier stamping for {} tier {} (reusing tier {})",
                    ownerId, damageTier, damageTier - 1);
            return copyNativeImage(prevImage);
        }

        private ResourceLocation generateComposited(String cacheKey, String dynamicPath) {
            NativeImage baseImage = SkinTextureReader.readTexture(texture);
            if (baseImage == null) {
                VisualHealth.LOGGER.debug("Failed to load base texture {} for compositing", texture);
                return null;
            }

            int w = baseImage.getWidth();
            int h = baseImage.getHeight();
            boolean[][] alphaMask = getOrGenerateAlphaMask(texture);

            NativeImage incrementalCanvas = findPreviousTierCopy(w, h);
            NativeImage canvas;
            int startTier;

            if (incrementalCanvas != null) {
                canvas = incrementalCanvas;
                startTier = damageTypes.length - 1;
            } else {
                canvas = new NativeImage(w, h, true);
                for (int y = 0; y < h; y++) {
                    for (int x = 0; x < w; x++) {
                        canvas.setPixelRGBA(x, y, baseImage.getPixelRGBA(x, y));
                    }
                }
                startTier = 0;
            }

            try {
                return stampAndRegister(canvas, w, h, alphaMask, baseImage,
                        seed, damageTypes, tintProvider, stampFilter,
                        densityMultiplier, cacheKey, dynamicPath, startTier);
            } finally {
                baseImage.close();
            }
        }

        private ResourceLocation generateOverlay(String cacheKey, String dynamicPath) {
            TextureSize texSize = AlphaMaskCache.getOrGenerateTextureSize(texture);
            int w = texSize != null ? texSize.width() : 64;
            int h = texSize != null ? texSize.height() : 64;

            boolean[][] alphaMask = texture != null ? getOrGenerateAlphaMask(texture) : null;

            NativeImage incrementalCanvas = findPreviousTierCopy(w, h);
            NativeImage canvas;
            int startTier;

            if (incrementalCanvas != null) {
                canvas = incrementalCanvas;
                startTier = damageTypes.length - 1;
            } else {
                canvas = new NativeImage(w, h, true);
                for (int y = 0; y < h; y++) {
                    for (int x = 0; x < w; x++) {
                        canvas.setPixelRGBA(x, y, 0x00000000);
                    }
                }
                startTier = 0;
            }

            return stampAndRegister(canvas, w, h, alphaMask, null,
                    seed, damageTypes, tintProvider, stampFilter,
                    densityMultiplier, cacheKey, dynamicPath, startTier);
        }
    }

    private static String buildCacheKey(String category, String ownerId,
                                        ResourceLocation texture, int tier,
                                        DamageType[] types, boolean isTransparent) {
        StringBuilder sb = new StringBuilder();
        sb.append(category);
        if (!isTransparent && texture != null) {
            sb.append("_").append(texture);
        }
        sb.append("_").append(ownerId);
        sb.append("_tier").append(tier);
        for (DamageType type : types) {
            sb.append("_").append(type.name());
        }
        return sb.toString();
    }

    private static String buildDynamicPath(String category, String ownerId,
                                           ResourceLocation texture, int tier,
                                           boolean isTransparent) {
        String ownerPath = ownerId.replace(':', '_').replace('-', '_');
        if (!isTransparent && texture != null) {
            return "dynamic/" + category + "/" + ownerPath + "/"
                    + texture.getPath().replace('/', '_') + "_tier" + tier;
        }
        return "dynamic/" + category + "/" + ownerPath + "/tier" + tier;
    }

    private static DamageType[] buildDamageTypesFromTracker(int entityId, int damageTier) {
        DamageType[] types = new DamageType[damageTier];
        for (int tier = 0; tier < damageTier; tier++) {
            types[tier] = EntityHealthTracker.getDamageTypeForTier(entityId, tier + 1);
        }
        return types;
    }

    private static ResourceLocation stampAndRegister(
            NativeImage canvas,
            int width, int height,
            boolean[][] alphaMask,
            NativeImage originalImage,
            long seed,
            DamageType[] damageTypes,
            ToIntFunction<DamageType> tintProvider,
            Predicate<DamageType> stampFilter,
            float densityMultiplier,
            String cacheKey,
            String dynamicTexturePath,
            int startTier) {

        int maxTiers = ConfigHelper.INSTANCE.damageTierCount;
        double areaScale = (width * height) / (double) (BASE_TEXTURE_SIZE * BASE_TEXTURE_SIZE);
        int densityPercent = ConfigHelper.INSTANCE.woundDensityPercentage;
        int baseWoundsPerTier = (densityPercent * 115) / 100;
        int woundsPerTier = (int) (baseWoundsPerTier * areaScale * 5.0 / maxTiers * densityMultiplier);

        VisualHealth.LOGGER.debug("Stamping {}x{} texture ({} tiers, {} wounds/tier, area scale: {})",
                width, height, damageTypes.length, woundsPerTier, String.format("%.2f", areaScale));

        var resourceManager = Minecraft.getInstance().getResourceManager();
        int woundIndex = 0;

        for (int tier = startTier; tier < damageTypes.length; tier++) {
            DamageType damageType = damageTypes[tier];

            if (!stampFilter.test(damageType)) {
                continue;
            }

            long tierSeed = seed;
            for (int t = 0; t <= tier; t++) {
                tierSeed = tierSeed * 31 + damageTypes[t].name().hashCode();
            }
            Random tierRandom = new Random(tierSeed);

            int[] tierCells = WoundTextureUtils.shuffleGrid(width, height, tierRandom);

            int woundTint = tintProvider.applyAsInt(damageType);

            for (int i = 0; i < woundsPerTier; i++) {
                try {
                    ResourceLocation woundAssetId = WoundAssetSelector.getRandomWoundTexture(damageType, tierRandom);

                    NativeImage woundAsset;
                    try (var resource = resourceManager.open(woundAssetId)) {
                        woundAsset = NativeImage.read(resource);
                    }

                    NativeImage tintedWound = TintUtils.applyTint(woundAsset, woundTint);

                    int[] position = WoundTextureUtils.getFuzzyGridPosition(width, height,
                            tintedWound.getWidth(), tintedWound.getHeight(),
                            tierCells, i, tierRandom);

                    float opacity = getWoundOpacity(damageType, tierRandom);
                    WoundTextureUtils.stampTexture(canvas, tintedWound, position[0], position[1], opacity);

                    VisualHealth.LOGGER.debug("Stamped wound {} (tier {}, {}, opacity: {}) at ({}, {})",
                            ++woundIndex, tier + 1, damageType, String.format("%.0f%%", opacity * 100), position[0], position[1]);

                    tintedWound.close();
                    woundAsset.close();

                } catch (Exception e) {
                    VisualHealth.LOGGER.error("Failed to load or stamp wound texture: {}", e.getMessage(), e);
                }
            }
        }

        if (alphaMask != null) {
            AlphaMaskCache.applyAlphaMaskToTexture(canvas, alphaMask, originalImage);
        }

        var textureManager = Minecraft.getInstance().getTextureManager();
        ResourceLocation dynamicTextureId = new ResourceLocation("visualhealth", dynamicTexturePath);

        DynamicTexture texture = new DynamicTexture(canvas);
        textureManager.register(dynamicTextureId, texture);

        TEXTURE_CACHE.put(cacheKey, dynamicTextureId);
        IMAGE_CACHE.put(cacheKey, canvas);

        return dynamicTextureId;
    }

    public static void clearEntityTiers(int entityId, int minTier, int maxTier) {
        List<String> keysToRemove = new ArrayList<>();

        for (String key : TEXTURE_CACHE.keySet()) {
            if (isEntityKey(key, entityId)) {
                int tier = extractTier(key);
                if (tier >= minTier && tier <= maxTier) {
                    keysToRemove.add(key);
                }
            }
        }

        var textureManager = Minecraft.getInstance().getTextureManager();
        int cleared = 0;
        for (String key : keysToRemove) {
            ResourceLocation loc = TEXTURE_CACHE.remove(key);
            IMAGE_CACHE.remove(key);
            if (loc != null) {
                try { textureManager.release(loc); } catch (Exception ignored) {}
            }
            cleared++;
        }

        if (cleared > 0) {
            VisualHealth.LOGGER.debug("Cleared {} texture cache entries for entity ID {} (tiers {}-{})",
                    cleared, entityId, minTier, maxTier);
        }
    }

    private static boolean isEntityKey(String key, int entityId) {
        return key.contains("_e" + entityId + "_tier");
    }

    private static int extractTier(String key) {
        int tierIdx = key.indexOf("_tier");
        if (tierIdx == -1) return -1;
        int tierStart = tierIdx + 5;
        int tierEnd = key.indexOf("_", tierStart);
        if (tierEnd == -1) tierEnd = key.length();
        try {
            return Integer.parseInt(key.substring(tierStart, tierEnd));
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    public static void clearAllCaches() {
        int cacheSize = TEXTURE_CACHE.size();

        var textureManager = Minecraft.getInstance().getTextureManager();
        for (ResourceLocation loc : TEXTURE_CACHE.values()) {
            try { textureManager.release(loc); } catch (Exception ignored) {}
        }

        TEXTURE_CACHE.clear();
        IMAGE_CACHE.clear();

        AlphaMaskCache.clearAllCaches();

        if (cacheSize > 0) {
            VisualHealth.LOGGER.info("Cleared {} texture cache entries on resource reload", cacheSize);
        }
    }

    public static void clearTextureCaches() {
        int cacheSize = TEXTURE_CACHE.size();

        var textureManager = Minecraft.getInstance().getTextureManager();
        for (ResourceLocation loc : TEXTURE_CACHE.values()) {
            try { textureManager.release(loc); } catch (Exception ignored) {}
        }

        TEXTURE_CACHE.clear();
        IMAGE_CACHE.clear();

        if (cacheSize > 0) {
            VisualHealth.LOGGER.info("Cleared {} texture cache entries on config change", cacheSize);
        }
    }

    public static Integer getCompositedTextureGLId(LivingEntity entity) {
        int tier = EntityHealthTracker.getDamageTier(entity.getId());
        if (tier <= 0) return null;

        ResourceLocation baseTexture = TextureLocator.getEntityTexture(entity);
        if (baseTexture == null) return null;

        DamageType[] types = buildDamageTypesFromTracker(entity.getId(), tier);
        String cacheKey = buildCacheKey("composite", "e" + entity.getId(), baseTexture, tier, types, false);
        ResourceLocation composited = TEXTURE_CACHE.get(cacheKey);
        if (composited == null) {
            composited = builder()
                    .category("composite")
                    .entity(entity)
                    .damageTier(tier)
                    .texture(baseTexture)
                    .damageTypes(types)
                    .composite()
                    .generate();
            if (composited == null) return null;
        }

        try {
            net.minecraft.client.renderer.texture.AbstractTexture tex =
                    Minecraft.getInstance().getTextureManager().getTexture(composited);
            return tex.getId();
        } catch (Exception e) {
            VisualHealth.LOGGER.debug("Failed to get GL ID for composited texture: {}", e.getMessage());
        }
        return null;
    }

    private static float getWoundOpacity(DamageType damageType, Random random) {
        if (damageType == DamageType.GENERIC) {
            return GENERIC_WOUND_MIN_OPACITY + random.nextFloat() * (GENERIC_WOUND_MAX_OPACITY - GENERIC_WOUND_MIN_OPACITY);
        }
        return WEAPON_WOUND_MIN_OPACITY + random.nextFloat() * (WEAPON_WOUND_MAX_OPACITY - WEAPON_WOUND_MIN_OPACITY);
    }

    public static void saveAllCachedTextures() {

        java.io.File outputDir = new java.io.File("VHDamage");
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        int savedCount = 0;
        for (Map.Entry<String, NativeImage> entry : IMAGE_CACHE.entrySet()) {
            String filename = entry.getKey().replaceAll("[^a-zA-Z0-9_-]", "_") + ".png";
            java.io.File outputFile = new java.io.File(outputDir, filename);

            try {
                entry.getValue().writeToFile(outputFile);
                savedCount++;
            } catch (Exception e) {
                VisualHealth.LOGGER.error("Failed to save texture {}: {}", filename, e.getMessage());
            }
        }

        VisualHealth.LOGGER.info("Saved {} texture(s) to VHDamage directory", savedCount);
    }
}
