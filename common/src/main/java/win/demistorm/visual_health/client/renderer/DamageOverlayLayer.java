package win.demistorm.visual_health.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import win.demistorm.visual_health.VisualHealth;
import win.demistorm.visual_health.client.DamageRenderCheck;
import win.demistorm.visual_health.client.entitymappings.DamageType;
import win.demistorm.visual_health.client.entitymappings.EntityDamageColors;
import win.demistorm.visual_health.client.damagestate.EntityHealthTracker;
import win.demistorm.visual_health.client.texture.TextureLocator;
import win.demistorm.visual_health.client.texture.WoundTextureGenerator;

public class DamageOverlayLayer<T extends LivingEntity, M extends EntityModel<T>>
        extends RenderLayer<T, M> {

    private static final int RENDER_DISTANCE = 96;
    private static final ResourceLocation FALLBACK_TEXTURE = new ResourceLocation("visualhealth", "damage/scratches/scratch1.png");

    public DamageOverlayLayer(RenderLayerParent<T, M> renderer) {
        super(renderer);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight,
                       T entity, float limbSwing, float limbSwingAmount, float partialTick,
                       float ageInTicks, float netHeadYaw, float headPitch) {

        double distanceToCameraSq = entity.distanceToSqr(
                net.minecraft.client.Minecraft.getInstance().gameRenderer.getMainCamera().getPosition());

        if (distanceToCameraSq > RENDER_DISTANCE * RENDER_DISTANCE) {
            return;
        }

        if (!DamageRenderCheck.shouldRender(entity, DamageRenderCheck.ALL)) {
            return;
        }

        if (win.demistorm.visual_health.client.compat.PhysicsModBridge.getCapturingEntity() != null) {
            return;
        }

        EntityDamageColors.DamageOverride override =
                EntityDamageColors.getOverride(entity.getType());

        if (override == null || !override.isEmissive()) {
            return;
        }

        int damageTier = EntityHealthTracker.getDamageTier(entity.getId());
        if (damageTier == 0) {
            return;
        }

        if (!EntityHealthTracker.hasWeaponTiers(entity.getId(), damageTier)) {
            return;
        }

        M model = getParentModel();

        try {
            ResourceLocation baseTexture = TextureLocator.getEntityTexture(entity);
            if (baseTexture == null) {
                return;
            }

            ResourceLocation woundTexture = WoundTextureGenerator.builder()
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

            int overlay = net.minecraft.client.renderer.entity.LivingEntityRenderer.getOverlayCoords(entity, 0.0f);

            poseStack.pushPose();

            RenderType renderType = RenderType.entityTranslucentEmissive(woundTexture);
            int finalPackedLight = LightTexture.FULL_BRIGHT;

            VertexConsumer vertexConsumer = bufferSource.getBuffer(renderType);
            model.renderToBuffer(poseStack, vertexConsumer, finalPackedLight, overlay, 1.0f, 1.0f, 1.0f, 1.0f);

            poseStack.popPose();

            VisualHealth.LOGGER.debug("Rendered emissive weapon-only damage overlay for {} (ID: {}) at tier {}",
                    entity.getName().getString(), entity.getId(), damageTier);

        } catch (Exception e) {
            VisualHealth.LOGGER.error("Failed to render emissive overlay for {}: {}",
                    entity.getName().getString(), e.getMessage());
        }
    }
}
