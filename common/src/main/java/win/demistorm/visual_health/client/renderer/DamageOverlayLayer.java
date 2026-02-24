package win.demistorm.visual_health.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.resources.Identifier;
import win.demistorm.visual_health.VisualHealth;
import win.demistorm.visual_health.client.EntityDamageColors;

// Render wounds based on health percentage
public class DamageOverlayLayer<S extends LivingEntityRenderState, M extends EntityModel<? super S>>
        extends RenderLayer<S, M> {

    private static final int RENDER_DISTANCE = 96;

    public DamageOverlayLayer(RenderLayerParent<S, M> renderer) {
        super(renderer);
        VisualHealth.LOGGER.debug("DamageOverlayLayer created for renderer: {}", renderer.getClass().getSimpleName());
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int packedLight,
                       S entityRenderState, float limbSwing, float limbSwingAmount) {

        net.minecraft.world.entity.LivingEntity entity = win.demistorm.visual_health.client.VisualHealthRenderContext.getCurrentEntity(entityRenderState);

        if (entityRenderState.distanceToCameraSq > RENDER_DISTANCE * RENDER_DISTANCE) {
            return;
        }

        if (entityRenderState.isInvisible) {
            return;
        }

        if (entity == null) {
            VisualHealth.LOGGER.warn("Entity context is null during render! EntityType: {}",
                    entityRenderState.entityType.getDescription().getString());
            return;
        }

        int entityId = entity.getId();
        String entityName = entity.getName().getString();

        net.minecraft.world.entity.MobCategory spawnCategory = entity.getType().getCategory();
        boolean isMonster = spawnCategory == net.minecraft.world.entity.MobCategory.MONSTER;

        if (!isMonster && !win.demistorm.visual_health.ConfigHelper.INSTANCE.damagePassiveMobs) {
            return;
        }

        if (entity instanceof net.minecraft.world.entity.npc.villager.AbstractVillager &&
                !win.demistorm.visual_health.ConfigHelper.INSTANCE.damageVillagers) {
            return;
        }

        int damageTier = win.demistorm.visual_health.client.EntityHealthTracker.getDamageTier(entityId);
        if (damageTier == 0) {
            return;
        }

        VisualHealth.LOGGER.info("Rendering damage overlay for {} (ID: {}) at tier {}",
                entityName, entityId, damageTier);

        M model = getParentModel();

        try {
            if (win.demistorm.visual_health.client.EMFDamageHelper.applyEMFDamageIfPresent(
                    model, entity, damageTier)) {

                VisualHealth.LOGGER.debug("EMF damage applied, skipping overlay render for {}", entityName);
                return;
            }
        } catch (NoClassDefFoundError e) {
        }

        win.demistorm.visual_health.client.TextureSizeIndex.TextureSize textureSizeInfo =
                win.demistorm.visual_health.client.TextureSizeIndex.getTextureSize(entity);
        int textureWidth = textureSizeInfo.width();
        int textureHeight = textureSizeInfo.height();

        VisualHealth.LOGGER.debug("Entity {} texture size: {}x{}",
                entityName, textureWidth, textureHeight);

        Identifier woundTexture = win.demistorm.visual_health.client.texture.WoundTextureGenerator.generateWoundedTexture(
                entity, damageTier, textureWidth, textureHeight);

        float modelScale = 1.0f;
        int overlay = LivingEntityRenderer.getOverlayCoords(entityRenderState, 0.0f);

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
            renderType = RenderTypes.entityTranslucentEmissive(woundTexture);
            finalPackedLight = LightTexture.FULL_BRIGHT;
        } else {
            renderType = RenderTypes.entityCutoutNoCullZOffset(woundTexture);
            finalPackedLight = packedLight;
        }

        int identityTint = 0xFFFFFFFF;

        submitNodeCollector.order(0).submitModel(model, entityRenderState, poseStack, renderType,
                finalPackedLight, overlay, identityTint, null, 0, null);

        poseStack.popPose();
    }
}
