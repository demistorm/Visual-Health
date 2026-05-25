package win.demistorm.visual_health.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.monster.dragon.EnderDragonModel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EnderDragonRenderer;
import net.minecraft.client.renderer.entity.state.EnderDragonRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
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

    @Shadow
    private EnderDragonModel model;

    @Unique
    private static final Identifier DRAGON_LOCATION =
            Identifier.withDefaultNamespace("textures/entity/enderdragon/dragon.png");

    @Unique
    private static final int RENDER_DISTANCE = 96;

    @Unique
    private static final Identifier FALLBACK_TEXTURE =
            Identifier.fromNamespaceAndPath("visualhealth", "damage/generic/generic1.png");

    @Unique
    private EnderDragon visualhealth$currentDragon;

    @Inject(method = "extractRenderState", at = @At("RETURN"))
    private void visualhealth$trackDragon(EnderDragon dragon, EnderDragonRenderState state,
                                           float partialTick, CallbackInfo ci) {
        EntityHealthTracker.updateEntityDamageTier(dragon);
        visualhealth$currentDragon = dragon;
    }

    @Inject(method = "submit", at = @At("HEAD"))
    private void visualhealth$setupSubmit(EnderDragonRenderState state, PoseStack poseStack,
                                           SubmitNodeCollector submitNodeCollector,
                                           CameraRenderState cameraRenderState, CallbackInfo ci) {
        if (visualhealth$currentDragon != null) {
            EntityHealthTracker.setCurrentRenderEntity(visualhealth$currentDragon);
            EntityHealthTracker.setCurrentRenderTexture(DRAGON_LOCATION);
        }
    }

    @Inject(method = "submit", at = @At("RETURN"))
    private void visualhealth$cleanup(CallbackInfo ci) {
        EntityHealthTracker.clearCurrentRenderEntity();
        EntityHealthTracker.clearCurrentRenderTexture();
        visualhealth$currentDragon = null;
    }

    @Inject(
            method = "submit",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/vertex/PoseStack;popPose()V"
            )
    )
    private void visualhealth$renderWounds(EnderDragonRenderState state, PoseStack poseStack,
                                            SubmitNodeCollector submitNodeCollector,
                                            CameraRenderState cameraRenderState, CallbackInfo ci) {
        if (visualhealth$currentDragon == null) return;
        if (state.deathTime > 0.0F) return;
        if (state.distanceToCameraSq > (double) (RENDER_DISTANCE * RENDER_DISTANCE)) return;

        if (!DamageRenderCheck.shouldRender(visualhealth$currentDragon, DamageRenderCheck.ALL)) return;

        int entityId = visualhealth$currentDragon.getId();
        int damageTier = EntityHealthTracker.getDamageTier(entityId);
        if (damageTier == 0) return;

        if (!EntityHealthTracker.hasWeaponTiers(entityId, damageTier)) return;

        try {
            Identifier woundTexture = WoundTextureGenerator.builder()
                    .category("weapons")
                    .entity(visualhealth$currentDragon)
                    .damageTier(damageTier)
                    .texture(DRAGON_LOCATION)
                    .transparent()
                    .stampFilter(type -> type != DamageType.GENERIC)
                    .densityMultiplier(1.0f / 3.0f)
                    .generate();

            if (woundTexture == null) woundTexture = FALLBACK_TEXTURE;

            RenderType renderType = RenderTypes.entityTranslucentEmissive(woundTexture);

            submitNodeCollector.order(0).submitModel(
                    this.model, state, poseStack, renderType,
                    LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
                    -1, null, 0,
                    null);

            VisualHealth.LOGGER.debug("Rendered emissive dragon wounds (ID: {}) at tier {}",
                    entityId, damageTier);
        } catch (Exception e) {
            VisualHealth.LOGGER.error("Failed to render dragon wounds: {}", e.getMessage());
        }
    }
}
