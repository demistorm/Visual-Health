package win.demistorm.visual_health.client.texture;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.HttpTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import win.demistorm.visual_health.VisualHealth;

import java.io.FileInputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class SkinTextureReader {

    private SkinTextureReader() {
    }

    private static final Map<ResourceLocation, NativeImage> CACHE = new ConcurrentHashMap<>();

    public static boolean canRead(ResourceLocation textureId) {
        if (CACHE.containsKey(textureId)) {
            return true;
        }

        try {
            ResourceManager rm = Minecraft.getInstance().getResourceManager();
            rm.open(textureId).close();
            return true;
        } catch (Exception ignored) {
        }

        TextureManager tm = Minecraft.getInstance().getTextureManager();
        AbstractTexture tex = tm.getTexture(textureId, null);
        if (tex == null) {
            return false;
        }

        if (tex instanceof DynamicTexture dynamicTexture) {
            return dynamicTexture.getPixels() != null;
        }

        if (tex instanceof HttpTexture httpTexture) {
            return httpTexture.file != null && httpTexture.file.exists();
        }

        return false;
    }

    public static NativeImage readTexture(ResourceLocation textureId) {
        if (CACHE.containsKey(textureId)) {
            NativeImage cached = CACHE.get(textureId);
            if (cached != null) {
                return copyImage(cached);
            }
        }

        NativeImage image = loadTexture(textureId);
        if (image != null) {
            CACHE.put(textureId, copyImage(image));
            return image;
        }

        return null;
    }

    private static NativeImage loadTexture(ResourceLocation textureId) {
        // Try resource pack first (works for mob textures, default skins)
        try {
            ResourceManager rm = Minecraft.getInstance().getResourceManager();
            try (var resource = rm.open(textureId)) {
                return NativeImage.read(resource);
            }
        } catch (Exception ignored) {
        }

        // Get registered texture (returns null if not registered, avoids auto-creating SimpleTexture)
        TextureManager tm = Minecraft.getInstance().getTextureManager();
        AbstractTexture tex = tm.getTexture(textureId, null);

        if (tex == null) {
            VisualHealth.LOGGER.warn("Could not load texture {} (not registered)", textureId);
            return null;
        }

        // Try DynamicTexture (catch any other mods' runtime changes too)
        if (tex instanceof DynamicTexture dynamicTexture) {
            NativeImage pixels = dynamicTexture.getPixels();
            if (pixels != null) {
                VisualHealth.LOGGER.debug("Read texture from DynamicTexture: {} ({}x{})",
                        textureId, pixels.getWidth(), pixels.getHeight());
                return copyImage(pixels);
            }
        }

        // Try HttpTexture disk cache (player skins)
        if (tex instanceof HttpTexture httpTexture) {
            java.io.File cacheFile = httpTexture.file;
            if (cacheFile != null && cacheFile.exists()) {
                try (FileInputStream fis = new FileInputStream(cacheFile)) {
                    NativeImage image = NativeImage.read(fis);
                    VisualHealth.LOGGER.debug("Read player skin from disk cache: {} ({}x{})",
                            cacheFile.getName(), image.getWidth(), image.getHeight());
                    return image;
                } catch (Exception e) {
                    VisualHealth.LOGGER.error("Failed to read skin texture from disk cache for {}: {}",
                            textureId, e.getMessage());
                }
            }
        }

        VisualHealth.LOGGER.debug("Could not load texture {} from resource pack or disk cache", textureId);
        return null;
    }

    private static NativeImage copyImage(NativeImage source) {
        int w = source.getWidth();
        int h = source.getHeight();
        NativeImage copy = new NativeImage(w, h, true);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                copy.setPixelRGBA(x, y, source.getPixelRGBA(x, y));
            }
        }
        return copy;
    }

    public static void clearCache() {
        int size = CACHE.size();
        for (NativeImage image : CACHE.values()) {
            try { image.close(); } catch (Exception ignored) {}
        }
        CACHE.clear();
        if (size > 0) {
            VisualHealth.LOGGER.info("Cleared {} skin texture cache entries", size);
        }
    }
}
