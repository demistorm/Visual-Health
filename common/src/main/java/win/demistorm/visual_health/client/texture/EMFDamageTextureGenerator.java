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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

// Generate damaged EMF variant textures
public class EMFDamageTextureGenerator {

    private EMFDamageTextureGenerator() {
    }

    private static final Map<String, ResourceLocation> DAMAGE_CACHE = new ConcurrentHashMap<>();

    private static final int BASE_TEXTURE_SIZE = 64;
    public static ResourceLocation generateDamagedVariant(
            ResourceLocation variantTexture,
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
                        damagedVariant.setPixelRGBA(x, y, variantImage.getPixelRGBA(x, y));
                    }
                }

                double areaScale = (variantImage.getWidth() * variantImage.getHeight()) /
                        (double) (BASE_TEXTURE_SIZE * BASE_TEXTURE_SIZE);

                int densityPercent = win.demistorm.visual_health.ConfigHelper.INSTANCE.woundDensityPercentage;
                int numTiers = ConfigHelper.INSTANCE.damageTierCount;
                int baseWoundsPerTier = (densityPercent * 115) / 100;

                int woundsPerTier = (int) (baseWoundsPerTier * areaScale * 5.0 / numTiers);

                int woundIndex = 0;
                for (int tier = 1; tier <= damageTier; tier++) {

                    DamageType damageType = EntityHealthTracker.getDamageTypeForTier(entity.getId(), tier);

                    long tierSeed = entity.getId();
                    for (int t = 1; t <= tier; t++) {
                        DamageType dt = EntityHealthTracker.getDamageTypeForTier(entity.getId(), t);
                        tierSeed = tierSeed * 31 + dt.name().hashCode();
                    }
                    Random tierRandom = new Random(tierSeed);

                    int[] tierCells = WoundTextureUtils.shuffleGrid(variantImage.getWidth(), variantImage.getHeight(), tierRandom);

                    int woundTint = TintCalculator.getTintForDamageType(damageType, entity);

                    for (int i = 0; i < woundsPerTier; i++) {
                        try {
                            ResourceLocation woundAssetId = WoundAssetSelector.getRandomWoundTexture(damageType, tierRandom);

                            NativeImage woundAsset;
                            try (var resource = resourceManager.open(woundAssetId)) {
                                woundAsset = NativeImage.read(resource);
                            }

                            NativeImage tintedWound = TintUtils.applyTint(woundAsset, woundTint);

                            int[] position = WoundTextureUtils.getFuzzyGridPosition(variantImage.getWidth(), variantImage.getHeight(),
                                    tintedWound.getWidth(), tintedWound.getHeight(),
                                    tierCells, i, tierRandom);

                            VisualHealth.LOGGER.debug("Stamping wound {} (tier {}, {}) at ({}, {})",
                                    ++woundIndex, tier, damageType, position[0], position[1]);

                        // Stamp the tinted wound onto the variant texture
                        WoundTextureUtils.stampTexture(damagedVariant, tintedWound, position[0], position[1]);

                            tintedWound.close();
                            woundAsset.close();

                        } catch (Exception e) {
                            VisualHealth.LOGGER.error("Failed to load or stamp wound texture: {}", e.getMessage(), e);
                        }
                    }
                }

                TextureManager textureManager = net.minecraft.client.Minecraft.getInstance().getTextureManager();
                ResourceLocation dynamicTextureId = ResourceLocation.fromNamespaceAndPath("visualhealth",
                        "dynamic/emf_damage/" + entity.getId() + "/" + variantTexture.getPath().replace('/', '_') + "_tier" + damageTier);

                DynamicTexture texture = new DynamicTexture(damagedVariant);

                textureManager.register(dynamicTextureId, texture);

                DAMAGE_CACHE.put(cacheKey, dynamicTextureId);

                variantImage.close();

                return dynamicTextureId;

            } catch (Exception e) {
                VisualHealth.LOGGER.error("Failed to generate EMF damaged variant: {}", e.getMessage(), e);
                return variantTexture;
            }
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
}
