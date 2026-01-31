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

import java.util.Random;

// Renders wound overlay on living entities based on health percentage
// Uses entityCutoutNoCull for performance (GPU-friendly batching)
// Compatible with ETF, Iris shaders, and all entity renderers
public class DamageOverlayLayer<S extends LivingEntityRenderState, M extends EntityModel<? super S>>
        extends RenderLayer<S, M> {

    private static final int RENDER_DISTANCE = 24; // Blocks

    public DamageOverlayLayer(RenderLayerParent<S, M> renderer) {
        super(renderer);
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int packedLight,
                       S entityRenderState, float limbSwing, float limbSwingAmount) {

        // Distance check - skip far entities for performance
        if (entityRenderState.distanceToCameraSq > RENDER_DISTANCE * RENDER_DISTANCE) {
            return;
        }

        // Invisibility check - skip invisible entities
        if (entityRenderState.isInvisible) {
            return;
        }

        // Get entity ID from state (entityType as fallback since state doesn't have ID field)
        int entityId = entityRenderState.entityType.hashCode();

        // Get damage tier from our tracker
        int damageTier = win.demistorm.visual_health.client.EntityHealthTracker.getDamageTier(entityId);
        if (damageTier == 0) {
            return; // Healthy, no damage overlay
        }

        // Get wound texture based on tier and entity UUID (consistent per entity)
        Random random = new Random(entityId);
        Identifier woundTexture = WoundAssetSelector.getRandomWoundTexture(damageTier, random);

        // Get parent model (the entity's actual model - creeper, zombie, etc.)
        M model = getParentModel();

        // Scale up slightly to prevent Z-fighting with base model
        poseStack.pushPose();
        poseStack.scale(1.002f, 1.002f, 1.002f);

        // Get RenderType with entityCutoutNoCull (GPU-friendly, no transparency)
        RenderType renderType = RenderTypes.entityCutoutNoCull(woundTexture);

        // Get overlay coordinates (for hurt flash effect)
        int overlay = LivingEntityRenderer.getOverlayCoords(entityRenderState, 0.0f);

        // Blood tint color (dark red-orange)
        int tint = 0xFF4010;

        // Submit the entity model again with our wound texture overlay!
        // This renders the entire entity model with our wound texture painted on top
        submitNodeCollector.order(0).submitModel(model, entityRenderState, poseStack, renderType,
                packedLight, overlay, tint, null, 0, null);

        poseStack.popPose();
    }
}
