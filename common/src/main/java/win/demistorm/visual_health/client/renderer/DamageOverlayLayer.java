package win.demistorm.visual_health.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import win.demistorm.visual_health.VisualHealth;
import win.demistorm.visual_health.client.DamageRenderCheck;
import win.demistorm.visual_health.client.compat.PhysicsModBridge;
import win.demistorm.visual_health.client.damagestate.EntityHealthTracker;
import win.demistorm.visual_health.client.entitymappings.DamageType;
import win.demistorm.visual_health.client.entitymappings.EntityDamageColors;
import win.demistorm.visual_health.client.texture.TextureLocator;
import win.demistorm.visual_health.client.texture.WoundTextureGenerator;

public class DamageOverlayLayer<S extends LivingEntityRenderState, M extends EntityModel<? super S>>
        extends RenderLayer<S, M> {

    private static final int RENDER_DISTANCE = 96;
    private static final Identifier FALLBACK_TEXTURE =
            Identifier.fromNamespaceAndPath("visualhealth", "damage/generic/generic1.png");

    public DamageOverlayLayer(RenderLayerParent<S, M> renderer) {
        super(renderer);
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int packedLight,
                       S entityRenderState, float limbSwing, float limbSwingAmount) {

        net.minecraft.world.entity.LivingEntity entity = EntityHealthTracker.getCurrentRenderEntity();
        if (entity == null) return;

        if (PhysicsModBridge.getCapturingEntity() != null) return;

        if (entityRenderState.distanceToCameraSq > RENDER_DISTANCE * RENDER_DISTANCE) return;
        if (entityRenderState.isInvisible) return;

        if (!DamageRenderCheck.shouldRender(entity, DamageRenderCheck.ALL)) return;

        EntityDamageColors.DamageOverride override = EntityDamageColors.getOverride(entity.getType());
        if (override == null || !override.isEmissive()) return;

        int damageTier = EntityHealthTracker.getDamageTier(entity.getId());
        if (damageTier == 0) return;

        if (!EntityHealthTracker.hasWeaponTiers(entity.getId(), damageTier)) return;

        M model = getParentModel();

        try {
            Identifier baseTexture = TextureLocator.getEntityTexture(entity);
            if (baseTexture == null) return;

            Identifier woundTexture = WoundTextureGenerator.builder()
                    .category("weapons")
                    .entity(entity)
                    .damageTier(damageTier)
                    .texture(baseTexture)
                    .transparent()
                    .stampFilter(type -> type != DamageType.GENERIC)
                    .generate();

            if (woundTexture == null) {
                woundTexture = FALLBACK_TEXTURE;
            }

            int overlay = net.minecraft.client.renderer.entity.LivingEntityRenderer
                    .getOverlayCoords(entityRenderState, 0.0f);

            RenderType renderType = RenderTypes.entityTranslucentEmissive(woundTexture);
            int finalPackedLight = LightTexture.FULL_BRIGHT;

            poseStack.pushPose();

            submitNodeCollector.order(0).submitModel(model, entityRenderState, poseStack, renderType,
                    finalPackedLight, overlay, -1, null, 0, null);
        if (isEmissive) {
            renderType = RenderTypes.entityTranslucentEmissive(woundTexture);
            finalPackedLight = 15728880;
        } else {
            renderType = RenderTypes.entityCutoutZOffset(woundTexture);
            finalPackedLight = packedLight;
        }

        int identityTint = 0xFFFFFFFF;

        submitNodeCollector.order(0).submitModel(model, entityRenderState, poseStack, renderType,
                finalPackedLight, overlay, identityTint, null, 0, null);
            submitNodeCollector.submitModel(model, entityRenderState, poseStack, renderType,
                    finalPackedLight, overlay, -1, null);

            poseStack.popPose();

            VisualHealth.LOGGER.debug("Rendered emissive weapon-only damage overlay for {} (ID: {}) at tier {}",
                    entity.getName().getString(), entity.getId(), damageTier);

        } catch (Exception e) {
            VisualHealth.LOGGER.error("Failed to render emissive overlay for {}: {}",
                    entity.getName().getString(), e.getMessage());
        }
    }
}
