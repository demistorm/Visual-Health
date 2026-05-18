package win.demistorm.visual_health.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import win.demistorm.visual_health.client.compat.CorpseCompat;
import win.demistorm.visual_health.client.damagestate.EntityHealthTracker;

@Mixin(EntityRenderDispatcher.class)
public class EntityRenderDispatcherMixin {

    @Inject(
            method = "render(Lnet/minecraft/world/entity/Entity;DDDFFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("HEAD")
    )
    private void visualhealth$onRenderHead(Entity entity, double x, double y, double z, float rotation,
                                           float partialTick, PoseStack poseStack,
                                           MultiBufferSource bufferSource, int packedLight,
                                           CallbackInfo ci) {
        if (CorpseCompat.isCorpseEntity(entity)) {
            CorpseCompat.setCorpseContext(entity.getUUID());
        }

        if (entity instanceof LivingEntity livingEntity) {
            if (!CorpseCompat.isCorpseRendering()) {
                EntityHealthTracker.updateEntityDamageTier(livingEntity);
            }
            EntityHealthTracker.setCurrentRenderEntity(livingEntity);
        }
    }

    @Inject(
            method = "render(Lnet/minecraft/world/entity/Entity;DDDFFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("RETURN")
    )
    private void visualhealth$onRenderReturn(Entity entity, double x, double y, double z, float rotation,
                                             float partialTick, PoseStack poseStack,
                                             MultiBufferSource bufferSource, int packedLight,
                                             CallbackInfo ci) {
        if (CorpseCompat.isCorpseEntity(entity)) {
            CorpseCompat.clearCorpseContext();
        }
        EntityHealthTracker.clearCurrentRenderEntity();
    }
}
