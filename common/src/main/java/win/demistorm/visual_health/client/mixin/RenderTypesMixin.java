package win.demistorm.visual_health.client.mixin;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import win.demistorm.visual_health.client.compat.CorpseCompat;
import win.demistorm.visual_health.client.damagestate.EntityHealthTracker;
import win.demistorm.visual_health.client.renderer.TextureSwapHelper;

@Mixin(value = RenderType.class, priority = 1100)
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
                    "entityCutoutNoCull(Lnet/minecraft/resources/ResourceLocation;)Lnet/minecraft/client/renderer/RenderType;",
                    "entityCutoutNoCullZOffset(Lnet/minecraft/resources/ResourceLocation;)Lnet/minecraft/client/renderer/RenderType;",
                    "entityTranslucent(Lnet/minecraft/resources/ResourceLocation;)Lnet/minecraft/client/renderer/RenderType;",
                    "entityTranslucentEmissive(Lnet/minecraft/resources/ResourceLocation;)Lnet/minecraft/client/renderer/RenderType;"
            },
            at = @At(value = "HEAD"),
            index = 0,
            argsOnly = true
    )
    private static ResourceLocation visualhealth$swapTexture(ResourceLocation texture) {
        ResourceLocation corpseResult = CorpseCompat.swapCorpseTexture(texture);
        if (corpseResult != null) return corpseResult;

        LivingEntity entity = EntityHealthTracker.getCurrentRenderEntity();
        if (entity == null) return texture;

        ResourceLocation swapped = TextureSwapHelper.swapTexture(texture, entity);
        return swapped != null ? swapped : texture;
    }
}
