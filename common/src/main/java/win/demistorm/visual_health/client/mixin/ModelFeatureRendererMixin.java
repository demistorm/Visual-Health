package win.demistorm.visual_health.client.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.LivingEntity;
import win.demistorm.visual_health.client.damagestate.EntityHealthTracker;
import win.demistorm.visual_health.client.damagestate.VisualHealthStateAccess;

@Mixin(ModelFeatureRenderer.class)
public class ModelFeatureRendererMixin {

    @Inject(method = "renderModel", at = @At("HEAD"))
    private void visualhealth$setupRender(CallbackInfo ci,
                                           @Local(ordinal = 0) SubmitNodeStorage.ModelSubmit<?> modelSubmit) {
        Object state = modelSubmit.state();
        if (state instanceof LivingEntityRenderState livingState) {
            VisualHealthStateAccess vhState = (VisualHealthStateAccess) (Object) livingState;
            EntityHealthTracker.setCurrentRenderEntity(vhState.visualhealth$getEntity());
        }
    }

    @Inject(method = "renderModel", at = @At("RETURN"))
    private void visualhealth$cleanupRender(CallbackInfo ci) {
        EntityHealthTracker.clearCurrentRenderEntity();
    }
}
