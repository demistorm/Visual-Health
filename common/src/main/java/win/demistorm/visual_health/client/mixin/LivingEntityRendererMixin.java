package win.demistorm.visual_health.client.mixin;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import win.demistorm.visual_health.VisualHealth;
import win.demistorm.visual_health.client.damagestate.EntityHealthTracker;
import win.demistorm.visual_health.client.renderer.VisualHealthRenderContext;

// Tracks entity health for the damage overlay RenderLayer
// Extracts render state to track entities during rendering
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin<T extends LivingEntity, S extends LivingEntityRenderState>
        extends EntityRenderer<T, S> {

    protected LivingEntityRendererMixin(EntityRendererProvider.Context context) {
        super(context);
    }

    // Track entity health during render state extraction
    @Inject(
            method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V",
            at = @At("RETURN")
    )
    private void visualhealth$trackEntityHealth(T entity, S state, float partialTick, CallbackInfo ci) {
        VisualHealth.LOGGER.debug("Mixin extractRenderState called for {} (ID: {})",
                entity.getName().getString(), entity.getId());

        // Update entity damage tier in our tracker
        EntityHealthTracker.updateEntityDamageTier(entity);

        // Store entity with render state as key for safe per-entity tracking
        VisualHealthRenderContext.setCurrentEntity(state, entity);

        // Store render state for potential future use
    }

    // Clean up entity context after rendering completes
    @Inject(
            method = "submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
            at = @At("RETURN")
    )
    private void visualhealth$cleanupAfterRender(S state, com.mojang.blaze3d.vertex.PoseStack poseStack,
                                                  net.minecraft.client.renderer.SubmitNodeCollector nodeCollector,
                                                  net.minecraft.client.renderer.state.level.CameraRenderState cameraRenderState,
                                                  CallbackInfo ci) {
        // Remove entity from map to prevent cross-contamination
        VisualHealthRenderContext.clearCurrentEntity(state);
    }
}
