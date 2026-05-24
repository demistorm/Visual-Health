package win.demistorm.visual_health.client.texture;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import win.demistorm.visual_health.VisualHealth;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AlphaMaskCache {

    private AlphaMaskCache() {
    }

    private static final Map<ResourceLocation, boolean[][]> ALPHA_CACHE = new ConcurrentHashMap<>();
    private static final Map<ResourceLocation, TextureSize> DIMENSION_CACHE = new ConcurrentHashMap<>();

    public static TextureSize getOrGenerateTextureSize(ResourceLocation textureId) {
        if (DIMENSION_CACHE.containsKey(textureId)) {
            return DIMENSION_CACHE.get(textureId);
        }

        TextureSize textureSize = generateTextureSize(textureId);
        if (textureSize != null) {
            DIMENSION_CACHE.put(textureId, textureSize);
        }
        return textureSize;
    }

    private static TextureSize generateTextureSize(ResourceLocation textureId) {
        try {
            NativeImage image = SkinTextureReader.readTexture(textureId);

            try (image) {
                if (image == null) return null;
                int width = image.getWidth();
                int height = image.getHeight();
                return new TextureSize(width, height);
            }
        } catch (Exception e) {
            VisualHealth.LOGGER.error("Failed to get texture dimensions for {}: {}",
                    textureId, e.getMessage());
            return null;
        }
    }

    public static boolean[][] getOrGenerateAlphaMask(ResourceLocation textureId) {
        if (ALPHA_CACHE.containsKey(textureId)) {
            return ALPHA_CACHE.get(textureId);
        }

        boolean[][] alphaMask = generateAlphaMask(textureId);
        if (alphaMask != null) {
            ALPHA_CACHE.put(textureId, alphaMask);
        }
        return alphaMask;
    }

    private static boolean[][] generateAlphaMask(ResourceLocation textureId) {
        try {
            NativeImage image = SkinTextureReader.readTexture(textureId);

            try (image) {
                if (image == null) return null;
                int width = image.getWidth();
                int height = image.getHeight();
                boolean[][] alphaMask = new boolean[width][height];

                for (int x = 0; x < width; x++) {
                    for (int y = 0; y < height; y++) {
                        int pixel = image.getPixelRGBA(x, y);
                        int alpha = (pixel >> 24) & 0xFF;
                        alphaMask[x][y] = alpha == 255;
                    }
                }

                return alphaMask;
            }
        } catch (Exception e) {
            VisualHealth.LOGGER.error("Failed to generate alpha mask for texture {}: {}",
                    textureId, e.getMessage());
            return null;
        }
    }

    public static void applyAlphaMaskToTexture(NativeImage woundTexture, boolean[][] alphaMask, @Nullable NativeImage originalImage) {
        int width = woundTexture.getWidth();
        int height = woundTexture.getHeight();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (x >= alphaMask.length || y >= alphaMask[0].length) {
                    woundTexture.setPixelRGBA(x, y, 0x00000000);
                    continue;
                }

                if (!alphaMask[x][y]) {
                    if (originalImage != null && x < originalImage.getWidth() && y < originalImage.getHeight()) {
                        int originalPixel = originalImage.getPixelRGBA(x, y);
                        int originalAlpha = (originalPixel >> 24) & 0xFF;
                        if (originalAlpha > 0) {
                            woundTexture.setPixelRGBA(x, y, originalPixel);
                            continue;
                        }
                    }
                    woundTexture.setPixelRGBA(x, y, 0x00000000);
                }
            }
        }
    }

    public static void clearAllCaches() {
        int alphaCacheSize = ALPHA_CACHE.size();
        int dimensionCacheSize = DIMENSION_CACHE.size();
        ALPHA_CACHE.clear();
        DIMENSION_CACHE.clear();
        SkinTextureReader.clearCache();

        if (alphaCacheSize > 0 || dimensionCacheSize > 0) {
            VisualHealth.LOGGER.info("Cleared {} alpha mask and {} dimension cache entries on resource reload",
                    alphaCacheSize, dimensionCacheSize);
        }
    }
}
