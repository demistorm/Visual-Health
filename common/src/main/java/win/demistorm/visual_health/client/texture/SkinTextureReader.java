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
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class SkinTextureReader {

    private SkinTextureReader() {
    }

    private static final Map<ResourceLocation, NativeImage> CACHE = new ConcurrentHashMap<>();
    private static final int RETRY_DELAY_TICKS = 20;
    private static final Map<ResourceLocation, Long> RETRY_SCHEDULE = new ConcurrentHashMap<>();
    private static final Set<ResourceLocation> RETRY_FAILED = ConcurrentHashMap.newKeySet();

    private static long currentGameTick() {
        var level = Minecraft.getInstance().level;
        return level != null ? level.getGameTime() : 0L;
    }

    public static boolean canRead(ResourceLocation textureId) {
        if (CACHE.containsKey(textureId)) {
            return true;
        }

        if (RETRY_FAILED.contains(textureId)) {
            return false;
        }

        Long retryAt = RETRY_SCHEDULE.get(textureId);
        if (retryAt != null && currentGameTick() < retryAt) {
            return false;
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

        if (RETRY_FAILED.contains(textureId)) {
            return null;
        }

        Long retryAt = RETRY_SCHEDULE.get(textureId);
        if (retryAt != null && currentGameTick() < retryAt) {
            return null;
        }

        NativeImage image = loadTexture(textureId);
        if (image != null) {
            RETRY_SCHEDULE.remove(textureId);
            CACHE.put(textureId, copyImage(image));
            return image;
        }

        handleLoadFailure(textureId);
        return null;
    }

    private static void handleLoadFailure(ResourceLocation textureId) {
        if (RETRY_SCHEDULE.containsKey(textureId)) {
            VisualHealth.LOGGER.warn("Could not load texture {}, giving up after retry", textureId);
            RETRY_SCHEDULE.remove(textureId);
            RETRY_FAILED.add(textureId);
        } else {
            VisualHealth.LOGGER.warn("Could not load texture {}, will retry in {} ticks", textureId, RETRY_DELAY_TICKS);
            RETRY_SCHEDULE.put(textureId, currentGameTick() + RETRY_DELAY_TICKS);
        }
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
        int cacheSize = CACHE.size();
        for (NativeImage image : CACHE.values()) {
            try { image.close(); } catch (Exception ignored) {}
        }
        CACHE.clear();

        int failedSize = RETRY_FAILED.size();
        RETRY_SCHEDULE.clear();
        RETRY_FAILED.clear();

        if (cacheSize > 0 || failedSize > 0) {
            VisualHealth.LOGGER.info("Cleared {} skin texture cache entries, {} retry failed textures",
                    cacheSize, failedSize);
        }
    }
}
