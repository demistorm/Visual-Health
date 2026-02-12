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

// Renders wound overlay on living entities based on health percentage
// Uses entityCutoutNoCull for performance (GPU-friendly batching)
// Compatible with ETF, Iris shaders, and all entity renderers
public class DamageOverlayLayer<S extends LivingEntityRenderState, M extends EntityModel<? super S>>
        extends RenderLayer<S, M> {

    private static final int RENDER_DISTANCE = 96; // Blocks

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

        // Check entity spawn category (Monster vs everything else)
        net.minecraft.world.entity.MobCategory spawnCategory = entity.getType().getCategory();
        boolean isMonster = spawnCategory == net.minecraft.world.entity.MobCategory.MONSTER;

        // Apply config-based filtering
        // If passive mobs are disabled, skip non-monster entities
        if (!isMonster && !win.demistorm.visual_health.ConfigHelper.INSTANCE.damagePassiveMobs) {
            if (VisualHealth.debugMode) {
                VisualHealth.LOGGER.debug("Skipping {} - passive mobs disabled, entity is not a monster", entityName);
            }
            return;
        }

        // If villagers are disabled, skip villager-type entities
        // Villagers and Wandering Traders extend AbstractVillager
        // Zombie Villagers do NOT extend AbstractVillager (so they get damaged normally if passive mobs are on)
        if (entity instanceof net.minecraft.world.entity.npc.villager.AbstractVillager &&
                !win.demistorm.visual_health.ConfigHelper.INSTANCE.damageVillagers) {
            if (VisualHealth.debugMode) {
                VisualHealth.LOGGER.debug("Skipping {} - villagers disabled", entityName);
            }
            return;
        }

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

        // Get parent model (the entity's actual model - creeper, zombie, etc.)
        M model = getParentModel();

        // Check for EMF model with texture overrides - apply damage directly to variant textures
        // If EMF damage is applied, skip the separate overlay render
        if (win.demistorm.visual_health.client.EMFDamageHelper.applyEMFDamageIfPresent(
                model, entity, damageTier)) {

            if (VisualHealth.debugMode) {
                VisualHealth.LOGGER.debug("EMF damage applied, skipping overlay render for {}", entityName);
            }

            // EMF damage is baked into the variant texture, no need for separate overlay
            return;
        }

        // Get entity's actual texture size with fallback chain
        // 1. Hardcoded map (fast path for vanilla), 2. Dynamic detection, 3. 64x64 default
        win.demistorm.visual_health.client.TextureSizeIndex.TextureSize textureSizeInfo =
                win.demistorm.visual_health.client.TextureSizeIndex.getTextureSize(entity);
        int textureWidth = textureSizeInfo.width();
        int textureHeight = textureSizeInfo.height();

        if (VisualHealth.debugMode) {
            VisualHealth.LOGGER.debug("Entity {} texture size: {}x{}",
                    entityName, textureWidth, textureHeight);
        }

        // Generate a composite wound texture sized for the entity
        Identifier woundTexture = win.demistorm.visual_health.client.texture.WoundTextureGenerator.generateWoundedTexture(
                entity, damageTier, textureWidth, textureHeight);

        if (VisualHealth.debugMode) {
            VisualHealth.LOGGER.debug("Generated wound texture: {}", woundTexture);
        }

        // Welp, the damage overlay's scale
        float modelScale = 1.0f;

        // Get overlay coordinates (required for submitModel, hurt flash disabled with 0.0f)
        int overlay = LivingEntityRenderer.getOverlayCoords(entityRenderState, 0.0f);

        // Check for entity-specific emissive setting
        EntityDamageColors.DamageOverride override =
                EntityDamageColors.getOverride(entity.getType());

        // Determine if entity should be emissive
        // NOTE: Tint is no longer applied here - wounds are pre-tinted during generation
        boolean isEmissive = override != null && override.isEmissive();

        if (VisualHealth.debugMode && isEmissive) {
            VisualHealth.LOGGER.debug("Entity {} is using emissive rendering",
                    entity.getName().getString());
        }

        // Scale up slightly to prevent Z-fighting with base model
        poseStack.pushPose();
        poseStack.scale(modelScale, modelScale, modelScale);

        // Determine render type based on emissive setting
        // Emissive entities use entityTranslucentEmissive for true glow effect
        // Non-emissive use entityCutoutNoCull for performance
        RenderType renderType;
        int finalPackedLight;

        if (isEmissive) {
            renderType = RenderTypes.entityTranslucentEmissive(woundTexture);
            finalPackedLight = LightTexture.FULL_BRIGHT; // Full brightness for glow
        } else {
            renderType = RenderTypes.entityCutoutNoCullZOffset(woundTexture);
            finalPackedLight = packedLight; // Normal lighting
        }

        if (VisualHealth.debugMode) {
            VisualHealth.LOGGER.debug("RenderType: {}, isEmissive: {}", renderType, isEmissive);
        }

        // CRITICAL: Pass white (0xFFFFFFFF) as tint since wounds are pre-tinted during generation
        // This allows different wound types to have different colors on the same texture
        int identityTint = 0xFFFFFFFF; // White = no color change

        // Submit the entity model again with our wound texture overlay!
        if (VisualHealth.debugMode) {
            VisualHealth.LOGGER.debug("Submitting model with wound overlay... (emissive: {})", isEmissive);
        }

        submitNodeCollector.order(0).submitModel(model, entityRenderState, poseStack, renderType,
                finalPackedLight, overlay, identityTint, null, 0, null);

        if (VisualHealth.debugMode) {
            VisualHealth.LOGGER.debug("Model submitted successfully");
        }

        poseStack.popPose();
    }
}
