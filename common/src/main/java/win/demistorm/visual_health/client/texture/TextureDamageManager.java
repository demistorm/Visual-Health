package win.demistorm.visual_health.client.texture;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.entity.LivingEntity;
import org.apache.logging.log4j.Logger;
import win.demistorm.visual_health.VisualHealth;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

// Manages texture caching and generation for damaged entity textures
public class TextureDamageManager {

    private static final Logger LOGGER = VisualHealth.LOGGER;
    private static final String MOD_ID = VisualHealth.MOD_ID;

    // Cache for damaged textures to avoid regenerating every frame
    // Key: CacheKey (textureId, entityId, tier, tint)
    // Value: Identifier of the damaged texture
    private static final Map<CacheKey, Identifier> textureCache = new ConcurrentHashMap<>();

    // Cache for original texture images to avoid reloading
    private static final Map<Identifier, NativeImage> originalImageCache = new ConcurrentHashMap<>();

    // Get or create a damaged texture for an entity
    public static Identifier getOrCreateDamagedTexture(
            LivingEntity entity,
            Identifier originalTexture,
            int damageTier,
            int tintArgb) {

        // If no damage, return original
        if (damageTier == 0) {
            return originalTexture;
        }

        // Create cache key
        CacheKey key = new CacheKey(originalTexture, entity.getId(), damageTier, tintArgb);

        // Return cached version if available
        if (textureCache.containsKey(key)) {
            return textureCache.get(key);
        }

        // Load original texture
        NativeImage baseImage = loadOrCacheOriginalTexture(originalTexture);
        if (baseImage == null) {
            LOGGER.warn("Failed to load original texture: {}, using original", originalTexture);
            return originalTexture;
        }

        // Copy base image (don't modify the cached original)
        NativeImage damagedImage = copyImage(baseImage);

        // Apply damage effects
        WoundStampRenderer.applyWoundsByTier(damagedImage, damageTier, tintArgb, entity.getId());

        // Register as dynamic texture
        Identifier damagedId = registerDynamicTexture(
                damagedImage,
                originalTexture,
                entity.getId(),
                damageTier
        );

        textureCache.put(key, damagedId);
        LOGGER.debug("Generated damaged texture: {} for entity: {} at tier: {}",
                damagedId, entity.getId(), damageTier);

        return damagedId;
    }

    // Load and cache original texture
    private static NativeImage loadOrCacheOriginalTexture(Identifier textureId) {
        return originalImageCache.computeIfAbsent(textureId, id -> {
            try {
                ResourceManager rm = Minecraft.getInstance().getResourceManager();
                Optional<Resource> resource = rm.getResource(id);

                if (resource.isPresent()) {
                    try (InputStream stream = resource.get().open()) {
                        return NativeImage.read(stream);
                    }
                } else {
                    LOGGER.error("Texture resource not found: {}", id);
                    return null;
                }
            } catch (IOException e) {
                LOGGER.error("Failed to load texture: " + id, e);
                return null;
            }
        });
    }

    // Copy a NativeImage
    private static NativeImage copyImage(NativeImage source) {
        NativeImage copy = new NativeImage(source.format(), source.getWidth(), source.getHeight(), false);
        // Manually copy pixels using access widener since copyTo() doesn't exist in 1.21.11
        for (int y = 0; y < source.getHeight(); y++) {
            for (int x = 0; x < source.getWidth(); x++) {
                int pixel = source.getPixelABGR(x, y);
                copy.setPixelABGR(x, y, pixel);
            }
        }
        return copy;
    }

    // Register a dynamic texture with Minecraft's texture manager
    private static Identifier registerDynamicTexture(
            NativeImage image,
            Identifier originalTexture,
            int entityId,
            int tier) {

        // Create unique identifier for the damaged texture
        String safePath = originalTexture.toString()
                .replace(":", "_")
                .replace("/", "_")
                .replace(".png", "");

        Identifier damagedId = Identifier.fromNamespaceAndPath(
                MOD_ID,
                "damaged/" + safePath + "/entity_" + entityId + "_t" + tier
        );

        // Register the dynamic texture using new 1.21.11 constructor
        // The new constructor takes a Supplier<String> for the texture ID and the NativeImage
        DynamicTexture dynamicTexture = new DynamicTexture(() -> damagedId.toString(), image);
        Minecraft.getInstance().getTextureManager().register(damagedId, dynamicTexture);

        return damagedId;
    }

    // Clear all caches (call on resource reload)
    public static void clearCache() {
        LOGGER.info("Clearing texture damage cache ({} textures, {} originals)",
                textureCache.size(), originalImageCache.size());

        // Clear texture cache
        textureCache.clear();

        // Close and clear original image cache
        for (NativeImage image : originalImageCache.values()) {
            if (image != null) {
                image.close();
            }
        }
        originalImageCache.clear();
    }

    // Get cache statistics (for debugging)
    public static String getCacheStats() {
        return String.format("TextureDamageManager Cache: %d damaged, %d originals",
                textureCache.size(), originalImageCache.size());
    }

    // Cache key for looking up damaged textures
    private record CacheKey(Identifier texture, int entityId, int tier, int tint) {
    }
}
