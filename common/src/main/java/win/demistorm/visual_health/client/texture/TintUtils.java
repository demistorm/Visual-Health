package win.demistorm.visual_health.client.texture;

public final class TintUtils {

    private TintUtils() {
    }

    public static int blendColors(int color1, int color2, float ratio) {
        int r1 = color1 & 0xFF;
        int g1 = (color1 >> 8) & 0xFF;
        int b1 = (color1 >> 16) & 0xFF;
        int r2 = color2 & 0xFF;
        int g2 = (color2 >> 8) & 0xFF;
        int b2 = (color2 >> 16) & 0xFF;

        int r = Math.round(r1 * (1.0f - ratio) + r2 * ratio);
        int g = Math.round(g1 * (1.0f - ratio) + g2 * ratio);
        int b = Math.round(b1 * (1.0f - ratio) + b2 * ratio);

        return 0xFF000000 | (b << 16) | (g << 8) | r;
    }
}
