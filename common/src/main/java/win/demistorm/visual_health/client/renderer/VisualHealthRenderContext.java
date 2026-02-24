package win.demistorm.visual_health.client.renderer;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.LivingEntity;

import java.util.WeakHashMap;

public final class VisualHealthRenderContext {

    private VisualHealthRenderContext() {
    }

    private static final WeakHashMap<LivingEntityRenderState, LivingEntity> ENTITY_MAP = new WeakHashMap<>();

    public static LivingEntity getCurrentEntity(LivingEntityRenderState renderState) {
        return ENTITY_MAP.get(renderState);
    }

    public static void setCurrentEntity(LivingEntityRenderState renderState, LivingEntity entity) {
        ENTITY_MAP.put(renderState, entity);
    }

    public static void clearCurrentEntity(LivingEntityRenderState renderState) {
        ENTITY_MAP.remove(renderState);
    }
}
