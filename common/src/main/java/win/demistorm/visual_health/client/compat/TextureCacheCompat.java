package win.demistorm.visual_health.client.compat;

import net.minecraft.resources.ResourceLocation;

public final class TextureCacheCompat {

    private TextureCacheCompat() {
    }

    public static ResourceLocation normalizeCacheKey(ResourceLocation rl) {
        if ("iceandfire".equals(rl.getNamespace()) && rl.getPath().startsWith("dragon_texture_")) {
            String stripped = stripTrailingBooleans(rl.getPath());
            if (stripped != rl.getPath()) {
                return new ResourceLocation(rl.getNamespace(), stripped);
            }
        }
        return rl;
    }

    private static String stripTrailingBooleans(String path) {
        String s = path;
        while (s.endsWith("true") || s.endsWith("false")) {
            s = s.endsWith("true") ? s.substring(0, s.length() - 4) : s.substring(0, s.length() - 5);
        }
        return s;
    }
}
