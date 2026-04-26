package win.demistorm.visual_health.client.mixin;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import win.demistorm.visual_health.client.renderer.BufferSourceSwapHelper;

@Mixin(targets = {
        "net.irisshaders.batchedentityrendering.impl.FullyBufferedMultiBufferSource",
        "net.raphimc.immediatelyfast.feature.core.BatchableBufferSource"
}, priority = 700)
public class CompatBufferSourceMixin {

    private static boolean vh$fired = false;

    @ModifyVariable(
            method = "getBuffer",
            at = @At(value = "HEAD"),
            index = 1,
            argsOnly = true
    )
    private RenderType visualhealth$swapTexture(RenderType renderType) {
        if (!vh$fired) {
            vh$fired = true;
            System.err.println("VH COMPAT FIRED on " + this.getClass().getName());
        }
        return BufferSourceSwapHelper.swapTexture(renderType);
    }
}
