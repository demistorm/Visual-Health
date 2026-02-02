package win.demistorm.visual_health.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
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

// Renders wound overlay on living entities based on health percentage
// Uses entityCutoutNoCull for performance (GPU-friendly batching)
// Compatible with ETF, Iris shaders, and all entity renderers
public class DamageOverlayLayer<S extends LivingEntityRenderState, M extends EntityModel<? super S>>
        extends RenderLayer<S, M> {

    private static final int RENDER_DISTANCE = 24; // Blocks

    public DamageOverlayLayer(RenderLayerParent<S, M> renderer) {
        super(renderer);
        VisualHealth.LOGGER.debug("DamageOverlayLayer created for renderer: {}", renderer.getClass().getSimpleName());
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int packedLight,
                       S entityRenderState, float limbSwing, float limbSwingAmount) {

        // Get the actual entity from render context using render state as key
        net.minecraft.world.entity.LivingEntity entity = win.demistorm.visual_health.client.VisualHealthRenderContext.getCurrentEntity(entityRenderState);

        // Distance check - skip far entities for performance
        if (entityRenderState.distanceToCameraSq > RENDER_DISTANCE * RENDER_DISTANCE) {
            if (VisualHealth.debugMode) {
                VisualHealth.LOGGER.debug("Skipping entity - too far (distance: {})",
                        (int)Math.sqrt(entityRenderState.distanceToCameraSq));
            }
            return;
        }

        // Invisibility check - skip invisible entities
        if (entityRenderState.isInvisible) {
            if (VisualHealth.debugMode) {
                VisualHealth.LOGGER.debug("Skipping entity - invisible");
            }
            return;
        }

        // Entity should always be available in context during rendering
        if (entity == null) {
            VisualHealth.LOGGER.warn("Entity context is null during render! EntityType: {}",
                    entityRenderState.entityType.getDescription().getString());
            return;
        }

        // Get actual entity ID and name
        int entityId = entity.getId();
        String entityName = entity.getName().getString();

        if (VisualHealth.debugMode) {
            VisualHealth.LOGGER.debug("DamageOverlayLayer.submit() called for {} (entityId: {})",
                    entityName, entityId);
        }

        // Get damage tier from our tracker
        int damageTier = win.demistorm.visual_health.client.EntityHealthTracker.getDamageTier(entityId);
        if (damageTier == 0) {
            if (VisualHealth.debugMode) {
                VisualHealth.LOGGER.debug("Skipping {} - healthy (tier 0)", entityName);
            }
            return; // Healthy, no damage overlay
        }

        VisualHealth.LOGGER.info("Rendering damage overlay for {} (ID: {}) at tier {}",
                entityName, entityId, damageTier);

        // Generate a composite wound texture sized for the entity
        // Most entities use 64x64 textures, but some may vary
        int textureSize = 64; // Default size for most mobs (creeper, zombie, etc.)
        Identifier woundTexture = win.demistorm.visual_health.client.texture.WoundTextureGenerator.generateWoundedTexture(
                entity, damageTier, textureSize, textureSize);

        if (VisualHealth.debugMode) {
            VisualHealth.LOGGER.debug("Generated wound texture: {}", woundTexture);
        }

        // Get parent model (the entity's actual model - creeper, zombie, etc.)
        M model = getParentModel();

        if (VisualHealth.debugMode) {
            VisualHealth.LOGGER.debug("Parent model class: {}", model.getClass().getSimpleName());
        }

        // Scale up slightly to prevent Z-fighting with base model
        poseStack.pushPose();
        poseStack.scale(1.002f, 1.002f, 1.002f);

        // Try out entityCutoutNoCullZOffset for performance
        RenderType renderType = RenderTypes.entityCutoutNoCullZOffset(woundTexture);

        if (VisualHealth.debugMode) {
            VisualHealth.LOGGER.debug("RenderType: {}", renderType);
        }

        // Get overlay coordinates (required for submitModel, hurt flash disabled with 0.0f)
        int overlay = LivingEntityRenderer.getOverlayCoords(entityRenderState, 0.0f);

        // Blood red tint - turns greyscale wounds into red wounds
        int tint = 0xFFFF4010;

        // Submit the entity model again with our wound texture overlay!
        // This renders the entire entity model with our wound texture painted on top
        if (VisualHealth.debugMode) {
            VisualHealth.LOGGER.debug("Submitting model with wound overlay...");
        }

        submitNodeCollector.order(0).submitModel(model, entityRenderState, poseStack, renderType,
                packedLight, overlay, tint, null, 0, null);

        if (VisualHealth.debugMode) {
            VisualHealth.LOGGER.debug("Model submitted successfully");
        }

        poseStack.popPose();
    }
}
