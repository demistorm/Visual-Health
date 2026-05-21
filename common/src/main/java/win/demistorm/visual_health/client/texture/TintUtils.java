package win.demistorm.visual_health.client.texture;

import com.mojang.blaze3d.platform.NativeImage;

public final class TintUtils {

    private TintUtils() {
    }

    // Blend two ARGB colors by ratio (0 = color1, 1 = color2)
    public static int blendColors(int color1, int color2, float ratio) {
        int r1 = (color1 >> 16) & 0xFF;
        int g1 = (color1 >> 8) & 0xFF;
        int b1 = color1 & 0xFF;
        int r2 = (color2 >> 16) & 0xFF;
        int g2 = (color2 >> 8) & 0xFF;
        int b2 = color2 & 0xFF;

        int r = Math.round(r1 * (1.0f - ratio) + r2 * ratio);
        int g = Math.round(g1 * (1.0f - ratio) + g2 * ratio);
        int b = Math.round(b1 * (1.0f - ratio) + b2 * ratio);

        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    // Tint a wound texture by multiplying each channel (ARGB format)
    public static NativeImage applyTint(NativeImage wound, int tint) {
        int tintR = (tint >> 16) & 0xFF;
        int tintG = (tint >> 8) & 0xFF;
        int tintB = tint & 0xFF;

        NativeImage tinted = new NativeImage(wound.getWidth(), wound.getHeight(), true);

        for (int y = 0; y < wound.getHeight(); y++) {
            for (int x = 0; x < wound.getWidth(); x++) {
                int pixel = wound.getPixel(x, y);
                int alpha = (pixel >> 24) & 0xFF;

                if (alpha == 0) {
                    continue;
                }

                int r = (pixel >> 16) & 0xFF;
                int g = (pixel >> 8) & 0xFF;
                int b = pixel & 0xFF;

                int tintedR = (r * tintR) / 255;
                int tintedG = (g * tintG) / 255;
                int tintedB = (b * tintB) / 255;

                int tintedPixel = (alpha << 24) | (tintedR << 16) | (tintedG << 8) | tintedB;
                tinted.setPixel(x, y, tintedPixel);
            }
        }

        return tinted;
    }
}
