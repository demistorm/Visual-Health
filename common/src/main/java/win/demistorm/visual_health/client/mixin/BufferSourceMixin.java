package win.demistorm.visual_health.client.mixin;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import win.demistorm.visual_health.client.renderer.BufferSourceSwapHelper;

@Mixin(value = MultiBufferSource.BufferSource.class, priority = 700)
public class BufferSourceMixin {

    @ModifyVariable(
            method = "getBuffer",
            at = @At(value = "HEAD"),
            index = 1,
            argsOnly = true
    )
    private RenderType visualhealth$swapTexture(RenderType renderType) {
        return BufferSourceSwapHelper.swapTexture(renderType);
    }
}
