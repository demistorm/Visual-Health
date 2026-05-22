package win.demistorm.visual_health.client.mixin;

import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import win.demistorm.visual_health.client.damagestate.EntityHealthTracker;
import win.demistorm.visual_health.client.renderer.TextureSwapHelper;

@Mixin(value = RenderTypes.class, priority = 1100)
public class RenderTypesMixin {

    @ModifyVariable(
            method = {
                    "entitySolid",
                    "eyes",
                    "energySwirl",
                    "entitySmoothCutout",
                    "itemEntityTranslucentCull",
                    "entityCutout",
                    "entityDecal",
                    "entityNoOutline",
                    "armorCutoutNoCull",
                    "entityShadow",
                    "entitySolidZOffsetForward",
                    "entityCutoutNoCull(Lnet/minecraft/resources/Identifier;Z)Lnet/minecraft/client/renderer/rendertype/RenderType;",
                    "entityCutoutNoCullZOffset(Lnet/minecraft/resources/Identifier;Z)Lnet/minecraft/client/renderer/rendertype/RenderType;",
                    "entityTranslucent(Lnet/minecraft/resources/Identifier;Z)Lnet/minecraft/client/renderer/rendertype/RenderType;",
                    "entityTranslucentEmissive(Lnet/minecraft/resources/Identifier;Z)Lnet/minecraft/client/renderer/rendertype/RenderType;"
            },
            at = @At(value = "HEAD"),
            index = 0,
            argsOnly = true
    )
    private static Identifier visualhealth$swapTexture(Identifier texture) {
        LivingEntity entity = EntityHealthTracker.getCurrentRenderEntity();
        if (entity == null) return texture;

        Identifier swapped = TextureSwapHelper.swapTexture(texture, entity);
        return swapped != null ? swapped : texture;
    }
}
