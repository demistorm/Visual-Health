package win.demistorm.visual_health.client;

import net.minecraft.resources.Identifier;
import win.demistorm.visual_health.VisualHealth;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stores per-entity wound textures for EMF compatibility.
 * Since EMF shares models per entity type, we need per-entity texture tracking.
 */
public final class EMFPerEntityTextures {

    // Map entity ID (as String) to its wound texture identifier
    // Using String instead of UUID for EMF compatibility
    private static final Map<String, Identifier> ENTITY_WOUND_TEXTURES = new ConcurrentHashMap<>();

    private EMFPerEntityTextures() {
        // Utility class - no instances
    }

    /**
     * Register a wound texture for a specific entity by ID.
     * This texture will be used during EMF rendering instead of the base variant.
     *
     * @param entityId The integer ID of the entity (as String)
     * @param woundTexture The wound texture identifier
     */
    public static void setWoundTextureById(String entityId, Identifier woundTexture) {
        ENTITY_WOUND_TEXTURES.put(entityId, woundTexture);
        if (VisualHealth.debugMode) {
            VisualHealth.LOGGER.debug("Registered wound texture {} for entity ID {}",
                    woundTexture, entityId);
        }
    }

    /**
     * Get the wound texture for a specific entity by ID.
     *
     * @param entityId The integer ID of the entity (as String)
     * @return The wound texture identifier, or null if none registered
     */
    public static Identifier getWoundTextureById(String entityId) {
        return ENTITY_WOUND_TEXTURES.get(entityId);
    }
}
