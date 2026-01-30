package win.demistorm.visual_health.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import win.demistorm.visual_health.client.texture.HealthTierCalculator;
import win.demistorm.visual_health.client.texture.TextureDamageManager;
import win.demistorm.visual_health.client.texture.WoundStampRenderer;

// Mixin to intercept entity textures and apply damage based on health
// Works with 1.21.11's render system
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin<T extends LivingEntity, S extends LivingEntityRenderState, M extends EntityModel<? super S>>
        extends EntityRenderer<T, S> {

    // Required constructor for Mixin
    protected LivingEntityRendererMixin(EntityRendererProvider.Context context) {
        super(context);
    }

    // Inject into extractRenderState to store the original texture before rendering
    // This is called before submit and gives us access to the entity
    @Inject(
            method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V",
            at = @At("RETURN"),
            require = 0
    )
    private void visualhealth$extractTextureState(T entity, S state, float partialTick, CallbackInfo ci) {
        // Don't do anything here, just log for now
        // The actual texture modification happens in getTextureLocation
    }

    // Inject into submit (the new 1.21.1 render method) to modify textures
    // This intercepts AFTER ETF or other mods have selected their texture variant
    @Inject(
            method = "submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/CameraRenderState;)V",
            at = @At("HEAD"),
            require = 0
    )
    private void visualhealth$modifyEntityTexture(S renderState, PoseStack poseStack,
                                                    SubmitNodeCollector nodeCollector, CameraRenderState cameraRenderState,
                                                    CallbackInfo ci) {
        // Can't directly modify texture here in 1.21.11's new render system
        // We'll use the getTextureLocation approach instead
    }

    // Intercept getTextureLocation to replace textures with damaged versions
    // This works with both old and new render systems in 1.21.11
    @Inject(
            method = "getTextureLocation(Lnet/minecraft/world/entity/LivingEntity;)Lnet/minecraft/resources/Identifier;",
            at = @At("RETURN"),
            cancellable = true,
            require = 0
    )
    private void visualhealth$modifyTextureLocation(T entity, CallbackInfoReturnable<Identifier> cir) {
        if (entity == null) {
            return;
        }

        // Calculate damage tier
        int damageTier = HealthTierCalculator.getDamageTier(entity);

        // If no damage needed, return original
        if (damageTier == 0) {
            return;
        }

        // Get the original texture that was about to be returned
        Identifier originalTexture = cir.getReturnValue();

        // Get or create damaged texture
        Identifier damagedTexture = TextureDamageManager.getOrCreateDamagedTexture(
                entity,
                originalTexture,
                damageTier,
                WoundStampRenderer.BLOOD_TINT
        );

        // Return the damaged texture instead
        if (damagedTexture != null) {
            cir.setReturnValue(damagedTexture);
        }
    }
}
