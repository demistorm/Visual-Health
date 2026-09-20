package win.demistorm.visual_health.client.mixin;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import win.demistorm.visual_health.client.damagestate.EntityHealthTracker;
import win.demistorm.visual_health.client.damagestate.VisualHealthStateAccess;

@Mixin(ModelFeatureRenderer.class)
public class ModelFeatureRendererMixin {

    @Inject(method = "prepareModel", at = @At("HEAD"))
    private void visualhealth$setupPrepareModel(ModelFeatureRenderer.Submit<?> submit, CallbackInfo ci) {
        Object state = submit.state();
        if (state instanceof LivingEntityRenderState livingState) {
            VisualHealthStateAccess vhState = (VisualHealthStateAccess) (Object) livingState;
            EntityHealthTracker.setCurrentRenderEntity(vhState.visualhealth$getEntity());
        }
    }

    @Inject(method = "prepareModel", at = @At("RETURN"))
    private void visualhealth$cleanupPrepareModel(CallbackInfo ci) {
        EntityHealthTracker.clearCurrentRenderEntity();
    }
}
