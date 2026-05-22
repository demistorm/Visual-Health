package win.demistorm.visual_health.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import win.demistorm.visual_health.client.damagestate.EntityHealthTracker;
import win.demistorm.visual_health.client.damagestate.VisualHealthStateAccess;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin<T extends LivingEntity, S extends LivingEntityRenderState, M extends EntityModel<S>>
        extends EntityRenderer<T, S> {

    protected LivingEntityRendererMixin(EntityRendererProvider.Context context) {
        super(context);
    }

    @Inject(
            method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V",
            at = @At("RETURN")
    )
    private void visualhealth$trackEntity(T entity, S state, float partialTick, CallbackInfo ci) {
        EntityHealthTracker.updateEntityDamageTier(entity);

        VisualHealthStateAccess vhState = (VisualHealthStateAccess) (Object) state;
        vhState.visualhealth$setEntity(entity);
    }

    @Inject(
            method = "submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/CameraRenderState;)V",
            at = @At("HEAD")
    )
    private void visualhealth$setupSubmit(S state, PoseStack poseStack,
                                           SubmitNodeCollector nodeCollector,
                                           CameraRenderState cameraRenderState,
                                           CallbackInfo ci) {
        VisualHealthStateAccess vhState = (VisualHealthStateAccess) (Object) state;
        EntityHealthTracker.setCurrentRenderEntity(vhState.visualhealth$getEntity());
    }

    @Inject(
            method = "submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
            at = @At("RETURN")
    )
    private void visualhealth$cleanupAfterRender(S state, com.mojang.blaze3d.vertex.PoseStack poseStack,
                                       net.minecraft.client.renderer.SubmitNodeCollector nodeCollector,
                                       net.minecraft.client.renderer.state.CameraRenderState cameraRenderState,
                                       CallbackInfo ci) {
        EntityHealthTracker.clearCurrentRenderEntity();
    }
}
