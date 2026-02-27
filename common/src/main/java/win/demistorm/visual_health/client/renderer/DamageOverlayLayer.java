package win.demistorm.visual_health.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import win.demistorm.visual_health.VisualHealth;
import win.demistorm.visual_health.client.entitymappings.EntityDamageColors;
import win.demistorm.visual_health.client.damagestate.EntityHealthTracker;
import win.demistorm.visual_health.client.emf.EMFDamageHelper;
import win.demistorm.visual_health.client.entitymappings.TextureSizeIndex;
import win.demistorm.visual_health.client.texture.TextureSize;

// Render wounds based on health percentage
public class DamageOverlayLayer<T extends LivingEntity, M extends EntityModel<T>>
        extends RenderLayer<T, M> {

    private static final int RENDER_DISTANCE = 96;

    public DamageOverlayLayer(RenderLayerParent<T, M> renderer) {
        super(renderer);
        VisualHealth.LOGGER.debug("DamageOverlayLayer created for renderer: {}", renderer.getClass().getSimpleName());
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

        if (entity.isInvisible()) {
            return;
        }

        int entityId = entity.getId();
        String entityName = entity.getName().getString();

        net.minecraft.world.entity.MobCategory spawnCategory = entity.getType().getCategory();
        boolean isMonster = spawnCategory == net.minecraft.world.entity.MobCategory.MONSTER;

        if (!isMonster && !win.demistorm.visual_health.ConfigHelper.INSTANCE.damagePassiveMobs) {
            return;
        }

        if (entity instanceof net.minecraft.world.entity.npc.AbstractVillager &&
                !win.demistorm.visual_health.ConfigHelper.INSTANCE.damageVillagers) {
            return;
        }

        int damageTier = EntityHealthTracker.getDamageTier(entityId);
        if (damageTier == 0) {
            return;
        }

        VisualHealth.LOGGER.debug("Rendering damage overlay for {} (ID: {}) at tier {}",
                entityName, entityId, damageTier);

        M model = getParentModel();

        try {
            if (EMFDamageHelper.applyEMFDamageIfPresent(
                    model, entity, damageTier)) {

                VisualHealth.LOGGER.debug("EMF damage applied, skipping overlay render for {}", entityName);
                return;
            }
        } catch (NoClassDefFoundError ignored) {
        }

        TextureSize textureSizeInfo =
                TextureSizeIndex.getTextureSize(entity);
        int textureWidth = textureSizeInfo.width();
        int textureHeight = textureSizeInfo.height();

        VisualHealth.LOGGER.debug("Entity {} texture size: {}x{}",
                entityName, textureWidth, textureHeight);

        ResourceLocation woundTexture = win.demistorm.visual_health.client.texture.WoundTextureGenerator.generateWoundedTexture(
                entity, damageTier, textureWidth, textureHeight);

        float modelScale = 1.0f;
        int overlay = LivingEntityRenderer.getOverlayCoords(entity, 0.0f);

        EntityDamageColors.DamageOverride override =
                EntityDamageColors.getOverride(entity.getType());

        boolean isEmissive = override != null && override.isEmissive();

        if (isEmissive) {
            VisualHealth.LOGGER.debug("Entity {} is using emissive rendering",
                    entity.getName().getString());
        }

        poseStack.pushPose();
        poseStack.scale(modelScale, modelScale, modelScale);

        RenderType renderType;
        int finalPackedLight;

        if (isEmissive) {
            renderType = RenderType.entityTranslucentEmissive(woundTexture);
            finalPackedLight = LightTexture.FULL_BRIGHT;
        } else {
            renderType = RenderType.entityCutoutNoCullZOffset(woundTexture);
            finalPackedLight = packedLight;
        }

        VertexConsumer vertexConsumer = bufferSource.getBuffer(renderType);
        model.renderToBuffer(poseStack, vertexConsumer, finalPackedLight, overlay, 1.0f, 1.0f, 1.0f, 1.0f);

        poseStack.popPose();
    }
}
