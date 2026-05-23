package win.demistorm.visual_health.client.texture;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import win.demistorm.visual_health.VisualHealth;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class SkinColorSampler {

    private SkinColorSampler() {
    }

    private static final Map<ResourceLocation, Integer> COLOR_CACHE = new ConcurrentHashMap<>();

    private static final int QUANTIZE_SHIFT = 6;
    private static final int QUANTIZE_MASK = 0x03;

    private static final int FALLBACK_TINT = 0xFF9F0000;

    public static int getSampledTint(LivingEntity entity) {
        ResourceLocation skinTexture = TextureLocator.getEntityTexture(entity);
        if (skinTexture == null) {
            VisualHealth.LOGGER.debug("SkinColorSampler: no texture for {}, using fallback tint",
                    entity.getName().getString());
            return FALLBACK_TINT;
        }

        return getSampledTint(skinTexture);
    }

    public static int getSampledTint(ResourceLocation skinTexture) {
        return COLOR_CACHE.computeIfAbsent(skinTexture, SkinColorSampler::sampleColor);
    }

    private static int sampleColor(ResourceLocation skinTexture) {
        NativeImage skin = SkinTextureReader.readTexture(skinTexture);

        try (skin) {
            if (skin == null) {
                VisualHealth.LOGGER.debug("SkinColorSampler: could not read skin {}, using fallback tint", skinTexture);
                return FALLBACK_TINT;
            }
            Map<Integer, int[]> buckets = new HashMap<>();

            for (int y = 0; y < skin.getHeight(); y++) {
                for (int x = 0; x < skin.getWidth(); x++) {
                    int pixel = skin.getPixel(x, y);
                    int alpha = (pixel >> 24) & 0xFF;
                    if (alpha < 128) continue;

                    int r = (pixel >> 16) & 0xFF;
                    int g = (pixel >> 8) & 0xFF;
                    int b = pixel & 0xFF;

                    int qr = (r >> QUANTIZE_SHIFT) & QUANTIZE_MASK;
                    int qg = (g >> QUANTIZE_SHIFT) & QUANTIZE_MASK;
                    int qb = (b >> QUANTIZE_SHIFT) & QUANTIZE_MASK;
                    int key = (qr << 4) | (qg << 2) | qb;

                    buckets.compute(key, (k, v) -> {
                        if (v == null) return new int[]{1, r, g, b};
                        v[0]++;
                        v[1] += r;
                        v[2] += g;
                        v[3] += b;
                        return v;
                    });
                }
            }

            if (buckets.isEmpty()) {
                VisualHealth.LOGGER.debug("SkinColorSampler: no opaque pixels in {}, using fallback tint", skinTexture);
                return FALLBACK_TINT;
            }

            List<int[]> sorted = new ArrayList<>(buckets.values());
            sorted.sort(Comparator.comparingInt(a -> a[0]));

            int[] pick = sorted.size() > 1 ? sorted.get(1) : sorted.get(0);

            int outR = Math.max(0, Math.min(255, pick[1] / pick[0]));
            int outG = Math.max(0, Math.min(255, pick[2] / pick[0]));
            int outB = Math.max(0, Math.min(255, pick[3] / pick[0]));

            int tint = (0xFF << 24) | (outR << 16) | (outG << 8) | outB;

            VisualHealth.LOGGER.info("SkinColorSampler: sampled tint #{} from skin {} ({} buckets, 2nd least common: {} pixels)",
                    String.format("%02X%02X%02X", outR, outG, outB),
                    skinTexture, buckets.size(), pick[0]);

            return tint;
        }
    }

    public static void clearCache() {
        int size = COLOR_CACHE.size();
        COLOR_CACHE.clear();
        if (size > 0) {
            VisualHealth.LOGGER.info("Cleared {} sampled skin color cache entries", size);
        }
    }
}
