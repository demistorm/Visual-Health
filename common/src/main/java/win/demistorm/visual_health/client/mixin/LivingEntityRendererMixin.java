package win.demistorm.visual_health.client.mixin;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import win.demistorm.visual_health.VisualHealth;
import win.demistorm.visual_health.client.damagestate.EntityHealthTracker;

// Tracks entity health for the damage overlay RenderLayer
// Updates entity damage tier before rendering
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin<T extends LivingEntity> extends EntityRenderer<T> {

    protected LivingEntityRendererMixin(EntityRendererProvider.Context context) {
        super(context);
    }

    // Track entity health before rendering
    @Inject(
            method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("HEAD")
    )
    private void visualhealth$trackEntityHealth(T entity, float entityYaw, float partialTick,
                                                 PoseStack poseStack, MultiBufferSource bufferSource,
                                                 int packedLight, CallbackInfo ci) {
        VisualHealth.LOGGER.debug("Mixin render called for {} (ID: {})",
                entity.getName().getString(), entity.getId());

        // Update entity damage tier in our tracker
        EntityHealthTracker.updateEntityDamageTier(entity);
    }
}
