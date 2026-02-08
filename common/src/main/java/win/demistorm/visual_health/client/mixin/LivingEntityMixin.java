package win.demistorm.visual_health.client.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import win.demistorm.visual_health.client.DamageEventHandler;

// Mixin to LivingEntity to detect damage on the client side
// This is the most reliable way for a client-side-only mod to track damage events
// Works on both Fabric and NeoForge through Architectury
@Mixin(LivingEntity.class)
public class LivingEntityMixin {

    // Inject into handleDamageEvent to track weapon types on the client
    // This method is called on the client when an entity takes damage
    @Inject(method = "handleDamageEvent", at = @At("HEAD"))
    private void visualHealth$onHandleDamageEvent(DamageSource source, CallbackInfo ci) {
        // Get the entity being hurt (this)
        LivingEntity entity = (LivingEntity) (Object) this;

        // Only track on client side for visual wound system
        if (entity.level().isClientSide()) {
            DamageEventHandler.onLivingDamage(entity, source);
        }
    }
}
