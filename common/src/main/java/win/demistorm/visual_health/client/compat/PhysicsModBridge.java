package win.demistorm.visual_health.client.compat;

import net.minecraft.world.entity.LivingEntity;

public final class PhysicsModBridge {

    private PhysicsModBridge() {}

    private static final ThreadLocal<LivingEntity> CAPTURING_ENTITY = new ThreadLocal<>();

    public static void setCapturingEntity(LivingEntity entity) {
        CAPTURING_ENTITY.set(entity);
    }

    public static LivingEntity getCapturingEntity() {
        return CAPTURING_ENTITY.get();
    }

    public static void clearCapturingEntity() {
        CAPTURING_ENTITY.remove();
    }
}
