package win.demistorm.visual_health.client.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import win.demistorm.visual_health.VisualHealth;
import win.demistorm.visual_health.client.compat.PhysicsModBridge;

@Mixin(targets = "net.diebuddies.physics.PhysicsMod")
public class PhysicsModCompatMixin {

    @Unique
    private static boolean vh$fired = false;

    @Inject(method = "blockifyEntity", at = @At("HEAD"))
    private static void visualhealth$beforeBlockify(Level level, LivingEntity entity, CallbackInfo ci) {
        if (!vh$fired) {
            vh$fired = true;
            VisualHealth.LOGGER.debug("VH COMPAT FIRED on PhysicsMod");
        }
        PhysicsModBridge.setCapturingEntity(entity);
    }

    @Inject(method = "blockifyEntity", at = @At("TAIL"))
    private static void visualhealth$afterBlockify(Level level, LivingEntity entity, CallbackInfo ci) {
        PhysicsModBridge.clearCapturingEntity();
    }
}
