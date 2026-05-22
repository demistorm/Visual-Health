package win.demistorm.visual_health.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import win.demistorm.visual_health.VisualHealth;
import win.demistorm.visual_health.client.DamageRenderCheck;
import win.demistorm.visual_health.client.compat.PhysicsModBridge;

@Mixin(targets = "net.diebuddies.opengl.TextureHelper")
public class PhysicsTextureHelperMixin {

    @Unique
    private static boolean vh$fired = false;

    @ModifyReturnValue(method = "getLoadedTextures", at = @At("RETURN"), remap = false)
    private static GpuTextureView visualhealth$overrideTexture(GpuTextureView original) {
        LivingEntity entity = PhysicsModBridge.getCapturingEntity();
        if (entity == null) return original;

        if (!vh$fired) {
            vh$fired = true;
            VisualHealth.LOGGER.debug("VH COMPAT FIRED on PhysicsTextureHelper");
        }

        if (!DamageRenderCheck.shouldRender(entity, DamageRenderCheck.DamageCheck.OVERRIDES, DamageRenderCheck.DamageCheck.CONFIG)) {
            return original;
        }

        GpuTextureView composited = PhysicsModBridge.getCompositedTextureView(entity);
        if (composited != null) {
            VisualHealth.LOGGER.debug("VH Physics: substituted composited texture for {}",
                    entity.getName().getString());
            return composited;
        }

        return original;
    }
}
