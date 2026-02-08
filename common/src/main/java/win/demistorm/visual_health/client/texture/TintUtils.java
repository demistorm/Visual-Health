package win.demistorm.visual_health.client.texture;

import com.mojang.blaze3d.platform.NativeImage;

// Shared utility for tinting wound textures
// Extracted from EMFDamageTextureGenerator for reuse across wound generation systems
public final class TintUtils {

    private TintUtils() {
        // Utility class - no instances
    }

    // Apply a tint color to a wound texture using multiply blending
    // Preserves the wound's alpha channel and luminance detail while applying the color
    // Returns a NEW tinted image (does not modify the original)
    //
    // This is used to turn greyscale wound assets into colored wounds
    // (white becomes bright red, black becomes dark red, etc.)
    //
    // @param wound The wound texture to tint (greyscale PNG)
    // @param tint The tint color in ARGB format
    // @return A new tinted wound texture (caller must close when done)
    public static NativeImage applyTint(NativeImage wound, int tint) {
        // Extract tint components from ARGB
        int tintR = (tint >> 16) & 0xFF;
        int tintG = (tint >> 8) & 0xFF;
        int tintB = tint & 0xFF;

        // Create a new image for the tinted wound
        NativeImage tinted = new NativeImage(wound.getWidth(), wound.getHeight(), true);

        for (int y = 0; y < wound.getHeight(); y++) {
            for (int x = 0; x < wound.getWidth(); x++) {
                int pixel = wound.getPixel(x, y);
                int alpha = (pixel >> 24) & 0xFF;

                // Skip fully transparent pixels (keep them transparent)
                if (alpha == 0) {
                    continue;
                }

                // Get original RGB from greyscale wound
                int r = (pixel >> 16) & 0xFF;
                int g = (pixel >> 8) & 0xFF;
                int b = pixel & 0xFF;

                // Apply tint using multiply blending
                // This preserves the wound details (shadows, highlights) while applying the color
                // White areas (255) become bright tint color
                // Black areas (0) become black shadows
                // Grey areas become darker tint (natural shading)
                int tintedR = (r * tintR) / 255;
                int tintedG = (g * tintG) / 255;
                int tintedB = (b * tintB) / 255;

                // Combine with original alpha
                int tintedPixel = (alpha << 24) | (tintedR << 16) | (tintedG << 8) | tintedB;
                tinted.setPixel(x, y, tintedPixel);
            }
        }

        return tinted;
    }
}
