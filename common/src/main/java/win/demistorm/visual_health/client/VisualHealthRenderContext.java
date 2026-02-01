package win.demistorm.visual_health.client;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.LivingEntity;
import win.demistorm.visual_health.VisualHealth;

import java.util.WeakHashMap;

// Per-entity render context using WeakHashMap for safe entity tracking
// Prevents wound textures from bleeding between entities during batched rendering
public final class VisualHealthRenderContext {

    private VisualHealthRenderContext() {
        // Utility class - no instances
    }

    // WeakHashMap for automatic cleanup and safe entity tracking
    // Uses LivingEntityRenderState as key to prevent cross-contamination
    private static final WeakHashMap<LivingEntityRenderState, LivingEntity> ENTITY_MAP = new WeakHashMap<>();

    // Get the entity for a specific render state
    public static LivingEntity getCurrentEntity(LivingEntityRenderState renderState) {
        return ENTITY_MAP.get(renderState);
    }

    // Store entity with its render state as key
    public static void setCurrentEntity(LivingEntityRenderState renderState, LivingEntity entity) {
        ENTITY_MAP.put(renderState, entity);
        if (entity != null && VisualHealth.debugMode) {
            VisualHealth.LOGGER.debug("Set entity context for {}: {} (Health: {}/{})",
                    renderState.hashCode(), entity.getName().getString(), entity.getHealth(), entity.getMaxHealth());
        }
    }

    // Remove entity from map after rendering
    public static void clearCurrentEntity(LivingEntityRenderState renderState) {
        ENTITY_MAP.remove(renderState);
    }
}
