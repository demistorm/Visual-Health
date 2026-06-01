package win.demistorm.visual_health.client.renderer;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.Resource;
import win.demistorm.visual_health.VisualHealth;
import win.demistorm.visual_health.client.entitymappings.DamageType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

// Select random wound textures by damage type
public class WoundAssetSelector {

    private static final String MODID = "visualhealth";
    private static final String TEXTURE_FOLDER = "damage";

    private static final Map<DamageType, List<ResourceLocation>> woundTextures = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, NativeImage> assetImageCache = new ConcurrentHashMap<>();
    private static boolean texturesLoaded = false;

    public static synchronized void loadTextures() {
        if (texturesLoaded) {
            return;
        }

        VisualHealth.LOGGER.info("Loading wound texture identifiers for Visual Health");

        for (DamageType damageType : DamageType.values()) {
            woundTextures.put(damageType, loadTexturesFromFolder(damageType));
        }

        int totalTextures = 0;
        for (List<ResourceLocation> list : woundTextures.values()) {
            totalTextures += list.size();
        }

        VisualHealth.LOGGER.info("Loaded {} wound texture identifiers across {} damage types",
                totalTextures, DamageType.values().length);

        texturesLoaded = true;
    }

    private static List<ResourceLocation> loadTexturesFromFolder(DamageType damageType) {

        ResourceManager resourceManager = Minecraft.getInstance().getResourceManager();
        String folderPath = TEXTURE_FOLDER + "/" + damageType.getFolderName();

        Predicate<ResourceLocation> predicate = id ->
                id.getNamespace().equals("visualhealth") &&
                        id.getPath().startsWith(folderPath + "/") &&
                        id.getPath().endsWith(".png");

        Map<ResourceLocation, Resource> foundResources = resourceManager.listResources(folderPath, predicate);
        List<ResourceLocation> textures = new ArrayList<>(foundResources.keySet());

        VisualHealth.LOGGER.debug("Found {} textures in folder {} for damage type {}",
                textures.size(), folderPath, damageType);

        return textures;
    }

    // Get a random wound texture based on damage type
    public static ResourceLocation getRandomWoundTexture(DamageType damageType, Random random) {
        if (!texturesLoaded) {
            loadTextures();
        }

        List<ResourceLocation> textures = woundTextures.get(damageType);
        if (textures == null || textures.isEmpty()) {
            VisualHealth.LOGGER.warn("No textures found for damage type: {}", damageType);
            return getFallbackTexture();
        }

        ResourceLocation texture = textures.get(random.nextInt(textures.size()));

        VisualHealth.LOGGER.debug("Selected texture: {} (from {} textures)",
                texture, textures.size());

        return texture;
    }

    public static NativeImage getCachedWoundAsset(ResourceLocation assetId) {
        NativeImage cached = assetImageCache.get(assetId);
        if (cached != null) {
            return copyAsset(cached);
        }

        try (var resource = Minecraft.getInstance().getResourceManager().open(assetId)) {
            NativeImage image = NativeImage.read(resource);
            assetImageCache.put(assetId, image);
            return copyAsset(image);
        } catch (IOException e) {
            VisualHealth.LOGGER.error("Failed to load wound asset {}: {}", assetId, e.getMessage());
            return null;
        }
    }

    private static NativeImage copyAsset(NativeImage source) {
        NativeImage copy = new NativeImage(source.getWidth(), source.getHeight(), true);
        for (int y = 0; y < source.getHeight(); y++) {
            for (int x = 0; x < source.getWidth(); x++) {
                copy.setPixelRGBA(x, y, source.getPixelRGBA(x, y));
            }
        }
        return copy;
    }

    // Fallback texture if none found
    private static ResourceLocation getFallbackTexture() {
        return new ResourceLocation(MODID, "damage/generic/generic1.png");
    }

    // Clean up on resource reload
    public static void cleanup() {
        VisualHealth.LOGGER.info("Cleaning up wound texture identifiers and asset cache");

        for (NativeImage image : assetImageCache.values()) {
            try { image.close(); } catch (Exception ignored) {}
        }
        assetImageCache.clear();

        texturesLoaded = false;
        woundTextures.clear();
    }
}
