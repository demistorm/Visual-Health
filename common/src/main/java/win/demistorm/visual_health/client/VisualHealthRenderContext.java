package win.demistorm.visual_health.client;

import net.minecraft.world.entity.LivingEntity;

// Thread-local context to track which entity is currently being rendered
// This allows us to map textures back to their entities during RenderType creation
public final class VisualHealthRenderContext {

    private VisualHealthRenderContext() {
        // Utility class - no instances
    }

    // ThreadLocal to store current entity being rendered
    // Each rendering thread has its own context
    private static final ThreadLocal<LivingEntity> CURRENT_ENTITY = new ThreadLocal<>();

    // Flag to control when texture modification is allowed
    // Prevents modifying textures for UI, GUI, or non-entity renders
    private static final ThreadLocal<Boolean> ALLOW_TEXTURE_MODIFY = ThreadLocal.withInitial(() -> false);

    // Get the current entity being rendered
    public static LivingEntity getCurrentEntity() {
        return CURRENT_ENTITY.get();
    }

    // Set the current entity being rendered
    public static void setCurrentEntity(LivingEntity entity) {
        CURRENT_ENTITY.set(entity);
        if (entity != null) {
            System.out.println("[VisualHealth] Set entity context: " + entity.getName().getString() + " (Health: " + entity.getHealth() + "/" + entity.getMaxHealth() + ")");
        }
    }

    // Clear the current entity
    public static void clearCurrentEntity() {
        LivingEntity entity = CURRENT_ENTITY.get();
        if (entity != null) {
            System.out.println("[VisualHealth] Clearing entity context: " + entity.getName().getString());
        }
        CURRENT_ENTITY.remove();
    }

    // Check if texture modification is allowed
    public static boolean isTextureModifyAllowed() {
        return ALLOW_TEXTURE_MODIFY.get();
    }

    // Enable texture modification
    public static void allowTextureModify() {
        ALLOW_TEXTURE_MODIFY.set(true);
    }

    // Disable texture modification
    public static void preventTextureModify() {
        ALLOW_TEXTURE_MODIFY.set(false);
    }
}
