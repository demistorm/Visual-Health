package win.demistorm.visual_health.client.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import win.demistorm.visual_health.client.damagestate.DamageEventHandler;

@Mixin(LivingEntity.class)
public class LivingEntityMixin {

    @Inject(method = "handleDamageEvent", at = @At("HEAD"))
    private void visualHealth$onHandleDamageEvent(DamageSource source, CallbackInfo ci) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (entity.level().isClientSide()) {
            DamageEventHandler.onLivingDamage(entity, source);
        }
    }
}
