package win.demistorm.visual_health.client.mixin;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import win.demistorm.visual_health.client.damagestate.VisualHealthStateAccess;

@Mixin(LivingEntityRenderState.class)
public class LivingEntityRenderStateMixin implements VisualHealthStateAccess {

    @Unique
    @Nullable
    private LivingEntity visualhealth$entity;

    @Override
    @Nullable
    public LivingEntity visualhealth$getEntity() {
        return visualhealth$entity;
    }

    @Override
    public void visualhealth$setEntity(@Nullable LivingEntity entity) {
        this.visualhealth$entity = entity;
    }
}
