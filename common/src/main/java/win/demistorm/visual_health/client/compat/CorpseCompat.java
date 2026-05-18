package win.demistorm.visual_health.client.compat;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import win.demistorm.visual_health.ConfigHelper;
import win.demistorm.visual_health.VisualHealth;
import win.demistorm.visual_health.client.entitymappings.DamageType;
import win.demistorm.visual_health.client.entitymappings.EntityDamageColors;
import win.demistorm.visual_health.client.renderer.BufferSourceSwapHelper;
import win.demistorm.visual_health.client.renderer.RenderTypeHelper;
import win.demistorm.visual_health.client.renderer.WoundAssetSelector;
import win.demistorm.visual_health.client.texture.AlphaMaskCache;
import win.demistorm.visual_health.client.texture.SkinTextureReader;
import win.demistorm.visual_health.client.texture.TintUtils;
import win.demistorm.visual_health.client.texture.WoundTextureUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class CorpseCompat {

    private static final int BASE_TEXTURE_SIZE = 64;
    private static final int CORPSE_TIER_COUNT = 10;

    // 60% sword/20% axe/20% generic
    private static final DamageType[] CORPSE_DAMAGE_TYPES = {
            DamageType.SWORD, DamageType.SWORD, DamageType.SWORD,
            DamageType.SWORD, DamageType.SWORD, DamageType.SWORD,
            DamageType.AXE, DamageType.AXE,
            DamageType.GENERIC, DamageType.GENERIC
    };

    // Brown bruise color for generic damage
    private static final int GENERIC_BRUISE_COLOR = 0xFF13458B;

    private CorpseCompat() {}

    static final Class<?> CORPSE_ENTITY_CLASS;

    static {
        Class<?> cls = null;
        try {
            cls = Class.forName("de.maxhenkel.corpse.entities.CorpseEntity");
            VisualHealth.LOGGER.info("Corpse mod detected - corpse damage rendering enabled");
        } catch (ClassNotFoundException ignored) {
            // Corpse mod not installed
        }
        CORPSE_ENTITY_CLASS = cls;
    }

    public static boolean isCorpseEntity(net.minecraft.world.entity.Entity entity) {
        return CORPSE_ENTITY_CLASS != null && CORPSE_ENTITY_CLASS.isInstance(entity);
    }

    private static final ThreadLocal<UUID> CORPSE_UUID = new ThreadLocal<>();

    public static void setCorpseContext(UUID corpseEntityUUID) {
        CORPSE_UUID.set(corpseEntityUUID);
    }

    public static void clearCorpseContext() {
        CORPSE_UUID.remove();
    }

    public static boolean isCorpseRendering() {
        return CORPSE_UUID.get() != null;
    }

    public static UUID getCorpseEntityUUID() {
        return CORPSE_UUID.get();
    }

    private static final Map<String, ResourceLocation> CORPSE_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, NativeImage> CORPSE_IMAGE_CACHE = new ConcurrentHashMap<>();

    public static RenderType handleCorpseTexture(RenderType renderType) {
        if (!isCorpseRendering()) return null;
        if (!ConfigHelper.INSTANCE.damagePlayers) return null;

        ResourceLocation texture = RenderTypeHelper.extractTexture(renderType);
        if (texture == null) return null;

        if (!isPlayerSkin(texture)) return null;

        if (RenderTypeHelper.shouldSkipRenderType(renderType)) return null;
        if (!BufferSourceSwapHelper.shouldSwapTexture(texture)) return null;
        if (!ConfigHelper.INSTANCE.drawOnOptifineEmissives && texture.getPath().endsWith("_e.png")) return null;

        try {
            UUID corpseUUID = getCorpseEntityUUID();
            ResourceLocation replacement = generateCorpseTexture(corpseUUID, texture);

            if (replacement != null) {
                RenderType swapped = RenderTypeHelper.createWithTexture(renderType, texture, replacement);
                VisualHealth.LOGGER.debug("Corpse swap: {} -> {} for corpse {}", texture, replacement, corpseUUID);
                return swapped;
            }
        } catch (Exception e) {
            VisualHealth.LOGGER.error("Failed to swap corpse texture: {}", e.getMessage(), e);
        }

        return null;
    }

    private static boolean isPlayerSkin(ResourceLocation texture) {
        return "minecraft".equals(texture.getNamespace()) && texture.getPath().startsWith("skins/");
    }

    private static int getTintForPlayerCorpse(DamageType damageType) {
        if (damageType == DamageType.GENERIC) {
            return GENERIC_BRUISE_COLOR;
        }

        EntityDamageColors.DamageOverride override = EntityDamageColors.getOverride(EntityType.PLAYER);
        if (override != null) {
            return override.tintColor();
        }

        return switch (ConfigHelper.INSTANCE.damageColor) {
            case RED -> 0xFF00009F;
            case BLACK -> 0xFF000000;
            case WHITE -> 0xFFFFFFFF;
        };
    }

    private static ResourceLocation generateCorpseTexture(UUID corpseUUID, ResourceLocation baseTexture) {
        String cacheKey = "corpse_" + corpseUUID + "_" + baseTexture;

        if (CORPSE_CACHE.containsKey(cacheKey)) {
            return CORPSE_CACHE.get(cacheKey);
        }

        try {
            NativeImage baseImage = SkinTextureReader.readTexture(baseTexture);
            if (baseImage == null) {
                VisualHealth.LOGGER.error("Failed to load base texture {} for corpse compositing", baseTexture);
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

            boolean[][] alphaMask = AlphaMaskCache.getOrGenerateAlphaMask(baseTexture);

            double areaScale = (textureWidth * textureHeight) / (double) (BASE_TEXTURE_SIZE * BASE_TEXTURE_SIZE);
            int densityPercent = ConfigHelper.INSTANCE.woundDensityPercentage;
            int numTiers = CORPSE_TIER_COUNT;
            int baseWoundsPerTier = (densityPercent * 115) / 100;
            int woundsPerTier = (int) (baseWoundsPerTier * areaScale * 5.0 / numTiers);

            VisualHealth.LOGGER.debug("Compositing {}x{} corpse texture for {} (area scale: {}) with {} wounds per tier ({} total tiers)",
                    textureWidth, textureHeight, corpseUUID, areaScale, woundsPerTier, numTiers);

            long baseSeed = corpseUUID.getLeastSignificantBits();
            var resourceManager = Minecraft.getInstance().getResourceManager();

            int woundIndex = 0;
            for (int tier = 1; tier <= numTiers; tier++) {
                DamageType damageType = CORPSE_DAMAGE_TYPES[tier - 1];

                long tierSeed = baseSeed;
                for (int t = 1; t <= tier; t++) {
                    tierSeed = tierSeed * 31 + CORPSE_DAMAGE_TYPES[t - 1].name().hashCode();
                }
                Random tierRandom = new Random(tierSeed);

                int[] tierCells = WoundTextureUtils.shuffleGrid(textureWidth, textureHeight, tierRandom);

                int woundTint = getTintForPlayerCorpse(damageType);

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

                        VisualHealth.LOGGER.debug("Stamped corpse wound {} (tier {}, {}) at ({}, {})",
                                ++woundIndex, tier, damageType, position[0], position[1]);

                        tintedWound.close();
                        woundAsset.close();

                    } catch (Exception e) {
                        VisualHealth.LOGGER.error("Failed to load or stamp corpse wound texture: {}", e.getMessage(), e);
                    }
                }
            }

            if (alphaMask != null) {
                AlphaMaskCache.applyAlphaMaskToTexture(composited, alphaMask);
            }

            var textureManager = Minecraft.getInstance().getTextureManager();
            ResourceLocation dynamicTextureId = new ResourceLocation("visualhealth",
                    "dynamic/corpse/" + corpseUUID + "/" + baseTexture.getPath().replace('/', '_'));

            DynamicTexture texture = new DynamicTexture(composited);
            textureManager.register(dynamicTextureId, texture);

            CORPSE_CACHE.put(cacheKey, dynamicTextureId);
            CORPSE_IMAGE_CACHE.put(cacheKey, composited);

            baseImage.close();

            VisualHealth.LOGGER.info("Generated corpse damage texture for {} ({} total wounds)", corpseUUID, woundIndex);

            return dynamicTextureId;

        } catch (Exception e) {
            VisualHealth.LOGGER.error("Failed to generate corpse texture: {}", e.getMessage(), e);
            return null;
        }
    }

    public static void clearCaches() {
        int cacheSize = CORPSE_CACHE.size();

        for (NativeImage image : CORPSE_IMAGE_CACHE.values()) {
            try { image.close(); } catch (Exception ignored) {}
        }

        CORPSE_CACHE.clear();
        CORPSE_IMAGE_CACHE.clear();

        if (cacheSize > 0) {
            VisualHealth.LOGGER.info("Cleared {} corpse texture cache entries", cacheSize);
        }
    }
}
