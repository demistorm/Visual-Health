package win.demistorm.visual_health.client.texture;

import com.mojang.blaze3d.platform.NativeImage;

public final class TintUtils {

    private TintUtils() {
    }

    public static NativeImage applyTint(NativeImage wound, int tint) {
        int tintR = (tint >> 16) & 0xFF;
        int tintG = (tint >> 8) & 0xFF;
        int tintB = tint & 0xFF;

        NativeImage tinted = new NativeImage(wound.getWidth(), wound.getHeight(), true);

        for (int y = 0; y < wound.getHeight(); y++) {
            for (int x = 0; x < wound.getWidth(); x++) {
                int pixel = wound.getPixelRGBA(x, y);
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
                tinted.setPixelRGBA(x, y, tintedPixel);
            }
        }

        return tinted;
    }
}
