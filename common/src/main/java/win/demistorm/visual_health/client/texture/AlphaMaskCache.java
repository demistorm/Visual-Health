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
            return ALPHA_CACHE.get(textureId);
        }

        boolean[][] alphaMask = generateAlphaMask(textureId);
        if (alphaMask != null) {
            ALPHA_CACHE.put(textureId, alphaMask);
        }
        return alphaMask;
    }

    private static boolean[][] generateAlphaMask(Identifier textureId) {
        try {
            ResourceManager resourceManager = net.minecraft.client.Minecraft.getInstance().getResourceManager();

            try (var resource = resourceManager.open(textureId)) {
                NativeImage image = NativeImage.read(resource);

                int width = image.getWidth();
                int height = image.getHeight();
                boolean[][] alphaMask = new boolean[width][height];

                for (int x = 0; x < width; x++) {
                    for (int y = 0; y < height; y++) {
                        int pixel = image.getPixel(x, y);
                        int alpha = (pixel >> 24) & 0xFF;
                        alphaMask[x][y] = alpha > 0;
                    }
                }

                image.close();

                if (VisualHealth.debugMode) {
                    VisualHealth.LOGGER.debug("Generated alpha mask for texture {} ({}x{})",
                            textureId, width, height);
                }

                return alphaMask;
            }
        } catch (Exception e) {
            VisualHealth.LOGGER.error("Failed to generate alpha mask for texture {}: {}",
                    textureId, e.getMessage());
            return null;
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
                    woundTexture.setPixel(x + posX, y + posY, 0x00000000);
                    continue;
                }

                if (!alphaMask[entityX][entityY]) {
                    woundTexture.setPixel(x + posX, y + posY, 0x00000000);
                }
            }
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
