package win.demistorm.visual_health.client.emf;

import net.minecraft.resources.ResourceLocation;
import win.demistorm.visual_health.VisualHealth;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class EMFPerEntityTextures {

    private static final Map<String, ResourceLocation> ENTITY_WOUND_TEXTURES = new ConcurrentHashMap<>();

    private EMFPerEntityTextures() {
    }

    public static void setWoundTextureById(String entityId, ResourceLocation woundTexture) {
        ENTITY_WOUND_TEXTURES.put(entityId, woundTexture);
        VisualHealth.LOGGER.debug("Registered wound texture {} for entity ID {}",
                woundTexture, entityId);
    }

    public static ResourceLocation getWoundTextureById(String entityId) {
        return ENTITY_WOUND_TEXTURES.get(entityId);
    }

    public static void clearAllCaches() {
        int cacheSize = ENTITY_WOUND_TEXTURES.size();
        ENTITY_WOUND_TEXTURES.clear();

        if (cacheSize > 0) {
            VisualHealth.LOGGER.info("Cleared {} EMF per-entity texture mappings on resource reload", cacheSize);
        }
    }
}
