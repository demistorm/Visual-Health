package win.demistorm.visual_health.client;

import net.minecraft.resources.Identifier;
import win.demistorm.visual_health.VisualHealth;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class EMFPerEntityTextures {

    private static final Map<String, Identifier> ENTITY_WOUND_TEXTURES = new ConcurrentHashMap<>();

    private EMFPerEntityTextures() {
    }

    public static void setWoundTextureById(String entityId, Identifier woundTexture) {
        ENTITY_WOUND_TEXTURES.put(entityId, woundTexture);
        VisualHealth.LOGGER.debug("Registered wound texture {} for entity ID {}",
                woundTexture, entityId);
    }

    public static Identifier getWoundTextureById(String entityId) {
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
