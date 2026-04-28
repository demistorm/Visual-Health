package win.demistorm.visual_health.client.texture;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
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
            if (image == null) return null;

            try {
                int width = image.getWidth();
                int height = image.getHeight();
                return new TextureSize(width, height);
            } finally {
                image.close();
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
            if (image == null) return null;

            try {
                int width = image.getWidth();
                int height = image.getHeight();
                boolean[][] alphaMask = new boolean[width][height];

                int visiblePixels = 0;
                int invisiblePixels = 0;

                for (int x = 0; x < width; x++) {
                    for (int y = 0; y < height; y++) {
                        int pixel = image.getPixelRGBA(x, y);
                        int alpha = (pixel >> 24) & 0xFF;
                        boolean isVisible = alpha == 255;
                        alphaMask[x][y] = isVisible;

                        if (isVisible) {
                            visiblePixels++;
                        } else {
                            invisiblePixels++;
                        }
                    }
                }

                return alphaMask;
            } finally {
                image.close();
            }
        } catch (Exception e) {
            VisualHealth.LOGGER.error("Failed to generate alpha mask for texture {}: {}",
                    textureId, e.getMessage());
            return null;
        }
    }

    public static void applyAlphaMaskToTexture(NativeImage woundTexture, boolean[][] alphaMask) {
        int width = woundTexture.getWidth();
        int height = woundTexture.getHeight();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (x >= alphaMask.length || y >= alphaMask[0].length) {
                    woundTexture.setPixelRGBA(x, y, 0x00000000);
                    continue;
                }

                if (!alphaMask[x][y]) {
                    woundTexture.setPixelRGBA(x, y, 0x00000000);
                }
            }
        }
    }

    public static void maskWoundOnInvisiblePixels(NativeImage woundTexture, boolean[][] alphaMask, int posX, int posY) {
        int width = woundTexture.getWidth();
        int height = woundTexture.getHeight();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int entityX = posX + x;
                int entityY = posY + y;

                if (entityX < 0 || entityX >= alphaMask.length ||
                        entityY < 0 || entityY >= alphaMask[0].length) {
                    woundTexture.setPixelRGBA(x + posX, y + posY, 0x00000000);
                    continue;
                }

                if (!alphaMask[entityX][entityY]) {
                    woundTexture.setPixelRGBA(x + posX, y + posY, 0x00000000);
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
