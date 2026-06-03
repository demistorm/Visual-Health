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
    private static final Map<ResourceLocation, boolean[]> CELL_GRID_CACHE = new ConcurrentHashMap<>();

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
                        int pixel = image.getPixel(x, y);
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

    @Nullable
    public static boolean[] getOrGenerateCellGrid(ResourceLocation textureId) {
        boolean[] cached = CELL_GRID_CACHE.get(textureId);
        if (cached != null) {
            return cached;
        }

        boolean[] cellGrid = generateCellGrid(textureId);
        if (cellGrid != null) {
            CELL_GRID_CACHE.put(textureId, cellGrid);
        }
        return cellGrid;
    }

    @Nullable
    private static boolean[] generateCellGrid(ResourceLocation textureId) {
        boolean[][] alphaMask = getOrGenerateAlphaMask(textureId);
        if (alphaMask == null) return null;

        int width = alphaMask.length;
        int height = alphaMask[0].length;
        int gridCols = width / 8;
        int gridRows = height / 8;
        boolean[] cellGrid = new boolean[gridRows * gridCols];

        for (int cellRow = 0; cellRow < gridRows; cellRow++) {
            for (int cellCol = 0; cellCol < gridCols; cellCol++) {
                int cellX = cellCol * 8;
                int cellY = cellRow * 8;
                boolean opaque = false;

                for (int dy = 0; dy < 8 && !opaque; dy++) {
                    for (int dx = 0; dx < 8 && !opaque; dx++) {
                        int px = cellX + dx;
                        int py = cellY + dy;
                        if (px < width && py < height && alphaMask[px][py]) {
                            opaque = true;
                        }
                    }
                }

                cellGrid[cellRow * gridCols + cellCol] = opaque;
            }
        }

        return cellGrid;
    }

    public static void applyAlphaMaskToTexture(NativeImage woundTexture, boolean[][] alphaMask, @Nullable NativeImage originalImage) {
        int width = woundTexture.getWidth();
        int height = woundTexture.getHeight();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (x >= alphaMask.length || y >= alphaMask[0].length) {
                    woundTexture.setPixel(x, y, 0x00000000);
                    continue;
                }

                if (!alphaMask[x][y]) {
                    if (originalImage != null && x < originalImage.getWidth() && y < originalImage.getHeight()) {
                        int originalPixel = originalImage.getPixel(x, y);
                        int originalAlpha = (originalPixel >> 24) & 0xFF;
                        if (originalAlpha > 0) {
                            woundTexture.setPixel(x, y, originalPixel);
                            continue;
                        }
                    }
                    woundTexture.setPixel(x, y, 0x00000000);
                }
            }
        }
    }

    public static void clearAllCaches() {
        int alphaCacheSize = ALPHA_CACHE.size();
        int dimensionCacheSize = DIMENSION_CACHE.size();
        int cellGridCacheSize = CELL_GRID_CACHE.size();
        ALPHA_CACHE.clear();
        DIMENSION_CACHE.clear();
        CELL_GRID_CACHE.clear();
        SkinTextureReader.clearCache();

        if (alphaCacheSize > 0 || dimensionCacheSize > 0 || cellGridCacheSize > 0) {
            VisualHealth.LOGGER.info("Cleared {} alpha mask, {} dimension, and {} cell grid cache entries on resource reload",
                    alphaCacheSize, dimensionCacheSize, cellGridCacheSize);
        }
    }
}
