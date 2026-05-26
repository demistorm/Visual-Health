package win.demistorm.visual_health.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EnderDragonRenderer;
import net.minecraft.client.renderer.entity.EnderDragonRenderer.DragonModel;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import win.demistorm.visual_health.VisualHealth;
import win.demistorm.visual_health.client.DamageRenderCheck;
import win.demistorm.visual_health.client.damagestate.EntityHealthTracker;
import win.demistorm.visual_health.client.entitymappings.DamageType;
import win.demistorm.visual_health.client.texture.WoundTextureGenerator;

@Mixin(EnderDragonRenderer.class)
public abstract class EnderDragonRendererMixin {

    @Unique
    private static final ResourceLocation DRAGON_LOCATION =
            new ResourceLocation("textures/entity/enderdragon/dragon.png");

    @Unique
    private static final ResourceLocation FALLBACK_TEXTURE =
            new ResourceLocation("visualhealth", "damage/scratches/scratch1.png");

    @Final
    @Shadow
    private DragonModel model;

    @Inject(
            method = "render(Lnet/minecraft/world/entity/boss/enderdragon/EnderDragon;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/vertex/PoseStack;popPose()V",
                    ordinal = 1
            )
    )
    private void visualhealth$renderWounds(EnderDragon dragon, float entityYaw, float partialTick,
                                            PoseStack poseStack, MultiBufferSource bufferSource,
                                            int packedLight, CallbackInfo ci) {
        if (dragon.dragonDeathTime > 0) return;

        double distanceSq = dragon.distanceToSqr(
                Minecraft.getInstance().gameRenderer.getMainCamera().getPosition());
        if (distanceSq > (double) (96 * 96)) return;

        if (!DamageRenderCheck.shouldRender(dragon, DamageRenderCheck.ALL)) return;

        int entityId = dragon.getId();
        int damageTier = EntityHealthTracker.getDamageTier(entityId);
        if (damageTier == 0) return;

        if (!EntityHealthTracker.hasWeaponTiers(entityId, damageTier)) return;

        try {
            ResourceLocation woundTexture = WoundTextureGenerator.builder()
                    .category("weapons")
                    .entity(dragon)
                    .damageTier(damageTier)
                    .texture(DRAGON_LOCATION)
                    .transparent()
                    .stampFilter(type -> type != DamageType.GENERIC)
                    .densityMultiplier(1.0f / 3.0f)
                    .generate();

            if (woundTexture == null) woundTexture = FALLBACK_TEXTURE;

            RenderType renderType = RenderType.entityTranslucentEmissive(woundTexture);
            VertexConsumer vertexConsumer = bufferSource.getBuffer(renderType);
            this.model.renderToBuffer(poseStack, vertexConsumer,
                    LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
                    1.0f, 1.0f, 1.0f, 1.0f);

            VisualHealth.LOGGER.debug("Rendered emissive dragon wounds (ID: {}) at tier {}",
                    entityId, damageTier);
        } catch (Exception e) {
            VisualHealth.LOGGER.error("Failed to render dragon wounds: {}", e.getMessage());
        }
    }
}
