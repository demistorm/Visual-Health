package win.demistorm.visual_health.client.texture;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import win.demistorm.visual_health.VisualHealth;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AlphaMaskCache {

    private AlphaMaskCache() {
        // Utility class - no instances
    }

    private static final Map<Identifier, boolean[][]> ALPHA_CACHE = new ConcurrentHashMap<>();

    public static boolean[][] getOrGenerateAlphaMask(Identifier textureId) {
        if (ALPHA_CACHE.containsKey(textureId)) {
            if (VisualHealth.debugMode) {
                VisualHealth.LOGGER.debug("Alpha mask cache HIT for texture {}", textureId);
            }
            return ALPHA_CACHE.get(textureId);
        }

        if (VisualHealth.debugMode) {
            VisualHealth.LOGGER.debug("Alpha mask cache MISS for texture {}, generating new mask", textureId);
        }

        boolean[][] alphaMask = generateAlphaMask(textureId);
        if (alphaMask != null) {
            ALPHA_CACHE.put(textureId, alphaMask);
        }
        return alphaMask;
    }

    private static boolean[][] generateAlphaMask(Identifier textureId) {
        try {
            if (VisualHealth.debugMode) {
                VisualHealth.LOGGER.debug("Starting alpha mask generation for texture {}", textureId);
            }

            ResourceManager resourceManager = net.minecraft.client.Minecraft.getInstance().getResourceManager();

            try (var resource = resourceManager.open(textureId)) {
                NativeImage image = NativeImage.read(resource);

                int width = image.getWidth();
                int height = image.getHeight();
                boolean[][] alphaMask = new boolean[width][height];

                int visiblePixels = 0;
                int invisiblePixels = 0;

                for (int x = 0; x < width; x++) {
                    for (int y = 0; y < height; y++) {
                        int pixel = image.getPixel(x, y);
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

                image.close();

                if (VisualHealth.debugMode) {
                    VisualHealth.LOGGER.debug("Generated alpha mask for texture {} ({}x{}) - threshold: alpha==255, visible: {}, invisible: {}",
                            textureId, width, height, visiblePixels, invisiblePixels);
                }

                return alphaMask;
            }
        } catch (Exception e) {
            VisualHealth.LOGGER.error("Failed to generate alpha mask for texture {}: {}",
                    textureId, e.getMessage());
            return null;
        }
    }

    public static void applyAlphaMaskToTexture(NativeImage woundTexture, boolean[][] alphaMask) {
        if (VisualHealth.debugMode) {
            VisualHealth.LOGGER.debug("Starting alpha mask application to entire texture ({}x{})",
                    woundTexture.getWidth(), woundTexture.getHeight());
        }

        int width = woundTexture.getWidth();
        int height = woundTexture.getHeight();
        int pixelsCleared = 0;
        int pixelsKept = 0;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (x >= alphaMask.length || y >= alphaMask[0].length) {
                    woundTexture.setPixel(x, y, 0x00000000);
                    pixelsCleared++;
                    continue;
                }

                if (!alphaMask[x][y]) {
                    woundTexture.setPixel(x, y, 0x00000000);
                    pixelsCleared++;
                } else {
                    pixelsKept++;
                }
            }
        }

        if (VisualHealth.debugMode) {
            VisualHealth.LOGGER.debug("Alpha mask application complete - cleared: {}, kept: {}, total: {}",
                    pixelsCleared, pixelsKept, width * height);
        }
    }

    public static void maskWoundOnInvisiblePixels(NativeImage woundTexture, boolean[][] alphaMask, int posX, int posY) {
        if (VisualHealth.debugMode) {
            VisualHealth.LOGGER.debug("Starting alpha masking at position ({}, {})", posX, posY);
        }

        int width = woundTexture.getWidth();
        int height = woundTexture.getHeight();
        int pixelsCleared = 0;
        int pixelsKept = 0;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int entityX = posX + x;
                int entityY = posY + y;

                if (entityX < 0 || entityX >= alphaMask.length ||
                        entityY < 0 || entityY >= alphaMask[0].length) {
                    woundTexture.setPixel(x + posX, y + posY, 0x00000000);
                    pixelsCleared++;
                    continue;
                }

                if (!alphaMask[entityX][entityY]) {
                    woundTexture.setPixel(x + posX, y + posY, 0x00000000);
                    pixelsCleared++;
                } else {
                    pixelsKept++;
                }
            }
        }

        if (VisualHealth.debugMode) {
            VisualHealth.LOGGER.debug("Alpha masking complete at position ({}, {}) - cleared: {}, kept: {}, total: {}",
                    posX, posY, pixelsCleared, pixelsKept, width * height);
        }
    }

    public static void clearAllCaches() {
        int cacheSize = ALPHA_CACHE.size();
        ALPHA_CACHE.clear();

        if (cacheSize > 0) {
            VisualHealth.LOGGER.info("Cleared {} alpha mask cache entries on resource reload", cacheSize);
        }
    }
}
