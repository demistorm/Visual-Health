package win.demistorm.visual_health.client.damagestate;

import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

public interface VisualHealthStateAccess {

    @Nullable
    LivingEntity visualhealth$getEntity();

    void visualhealth$setEntity(@Nullable LivingEntity entity);
}
