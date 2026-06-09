package win.demistorm.visual_health.client.texture;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.HttpTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import org.lwjgl.opengl.GL11;
import win.demistorm.visual_health.VisualHealth;
import win.demistorm.visual_health.client.compat.TextureCacheCompat;

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

    public static ResourceLocation normalizeForCache(ResourceLocation rl) {
        return TextureCacheCompat.normalizeCacheKey(rl);
    }

    public static boolean canRead(ResourceLocation textureId) {
        ResourceLocation cacheKey = normalizeForCache(textureId);
        if (CACHE.containsKey(cacheKey)) {
            return true;
        }

        if (RETRY_FAILED.contains(cacheKey)) {
            return false;
        }

        Long retryAt = RETRY_SCHEDULE.get(cacheKey);
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

        // Any other registered AbstractTexture (ArrayLayeredTextures from Ice and Fire for instance) can be read via GL readback
        if (RenderSystem.isOnRenderThreadOrInit()) {
            try {
                return tex.getId() >= 0;
            } catch (Exception ignored) {}
        }

        return false;
    }

    public static NativeImage readTexture(ResourceLocation textureId) {
        ResourceLocation cacheKey = normalizeForCache(textureId);
        if (CACHE.containsKey(cacheKey)) {
            NativeImage cached = CACHE.get(cacheKey);
            if (cached != null) {
                return copyImage(cached);
            }
        }

        if (RETRY_FAILED.contains(cacheKey)) {
            return null;
        }

        Long retryAt = RETRY_SCHEDULE.get(cacheKey);
        if (retryAt != null && currentGameTick() < retryAt) {
            return null;
        }

        NativeImage image = loadTexture(textureId);
        if (image != null) {
            RETRY_SCHEDULE.remove(cacheKey);
            CACHE.put(cacheKey, copyImage(image));
            return image;
        }

        handleLoadFailure(textureId);
        return null;
    }

    private static void handleLoadFailure(ResourceLocation textureId) {
        ResourceLocation cacheKey = normalizeForCache(textureId);
        if (RETRY_SCHEDULE.containsKey(cacheKey)) {
            VisualHealth.LOGGER.warn("Could not load texture {}, giving up after retry", textureId);
            RETRY_SCHEDULE.remove(cacheKey);
            RETRY_FAILED.add(cacheKey);
        } else {
            VisualHealth.LOGGER.warn("Could not load texture {}, will retry in {} ticks", textureId, RETRY_DELAY_TICKS);
            RETRY_SCHEDULE.put(cacheKey, currentGameTick() + RETRY_DELAY_TICKS);
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

        /* Fallback: GL texture readback for custom AbstractTextures like the ArrayLayeredTexture
        from Ice and Fire's dragons
         */
        return readFromGLTexture(textureId, tex);
    }

    private static NativeImage readFromGLTexture(ResourceLocation textureId, AbstractTexture tex) {
        if (!RenderSystem.isOnRenderThreadOrInit()) {
            return null;
        }

        int glId;
        try {
            glId = tex.getId();
        } catch (Exception e) {
            return null;
        }
        if (glId < 0) {
            return null;
        }

        int prevBinding = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, glId);

        try {
            int width = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_WIDTH);
            int height = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_HEIGHT);

            if (width <= 0 || height <= 0) {
                VisualHealth.LOGGER.debug("GL readback: invalid dimensions {}x{} for {}", width, height, textureId);
                return null;
            }

            NativeImage image = new NativeImage(width, height, true);
            image.downloadTexture(0, false);

            VisualHealth.LOGGER.info("Read texture from GL readback: {} ({}x{})", textureId, width, height);
            return image;
        } catch (Exception e) {
            VisualHealth.LOGGER.debug("GL readback failed for {}: {}", textureId, e.getMessage());
            return null;
        } finally {
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, prevBinding);
        }
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
