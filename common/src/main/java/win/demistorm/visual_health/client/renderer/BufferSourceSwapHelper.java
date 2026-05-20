package win.demistorm.visual_health.client.renderer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import win.demistorm.visual_health.ConfigHelper;
import win.demistorm.visual_health.VisualHealth;
import win.demistorm.visual_health.client.DamageRenderCheck;
import win.demistorm.visual_health.client.compat.CorpseCompat;
import win.demistorm.visual_health.client.damagestate.EntityHealthTracker;
import win.demistorm.visual_health.client.texture.WoundTextureGenerator;

public final class BufferSourceSwapHelper {

    private static final int RENDER_DISTANCE_SQ = 96 * 96;

    private BufferSourceSwapHelper() {}

    public static RenderType swapTexture(RenderType renderType) {
        RenderType corpseResult = CorpseCompat.handleCorpseTexture(renderType);
        if (corpseResult != null) return corpseResult;

        LivingEntity entity = EntityHealthTracker.getCurrentRenderEntity();
        if (entity == null) {
            // if (debugLogCounter < 5) VisualHealth.LOGGER.debug("VH swap: entity is null");
            return renderType;
        }

        if (!DamageRenderCheck.shouldRender(entity, DamageRenderCheck.ALL)) {
            return renderType;
        }

        int damageTier = EntityHealthTracker.getDamageTier(entity.getId());
        if (damageTier == 0) return renderType;

        ResourceLocation texture = RenderTypeHelper.extractTexture(renderType);
        if (texture == null) {
            VisualHealth.LOGGER.debug("VH swap: {} tier={} but extractTexture returned null for rt={}",
                    entity.getName().getString(), damageTier, renderType);
            return renderType;
        }

        if (RenderTypeHelper.shouldSkipRenderType(renderType)) {
            VisualHealth.LOGGER.debug("VH swap: {} tier={} skipped render type {}",
                    entity.getName().getString(), damageTier, renderType);
            return renderType;
        }

        if (shouldSkipTexture(texture)) {
            VisualHealth.LOGGER.debug("VH swap: {} tier={} texture {} filtered by path",
                    entity.getName().getString(), damageTier, texture);
            return renderType;
        }

        if (!ConfigHelper.INSTANCE.drawOnOptifineEmissives && texture.getPath().endsWith("_e.png")) {
            return renderType;
        }

        if (entity.distanceToSqr(Minecraft.getInstance().gameRenderer.getMainCamera().getPosition())
                > RENDER_DISTANCE_SQ) {
            VisualHealth.LOGGER.debug("VH swap: {} too far away", entity.getName().getString());
            return renderType;
        }

        VisualHealth.LOGGER.debug("VH swap: attempting composite for {} tier={} texture={}",
                entity.getName().getString(), damageTier, texture);

        try {
            ResourceLocation replacement = WoundTextureGenerator.builder()
                    .category("composite")
                    .entity(entity)
                    .damageTier(damageTier)
                    .texture(texture)
                    .composite()
                    .generate();

            if (replacement != null) {
                RenderType swapped = RenderTypeHelper.createWithTexture(renderType, texture, replacement);
                VisualHealth.LOGGER.debug("Swapped texture {} -> {} for {} (ID: {})",
                        texture, replacement, entity.getName().getString(), entity.getId());
                return swapped;
            } else {
                VisualHealth.LOGGER.debug("VH swap: generateCompositedTexture returned null for {}",
                        entity.getName().getString());
            }
        } catch (Exception e) {
            VisualHealth.LOGGER.error("Failed to swap texture for {}: {}",
                    entity.getName().getString(), e.getMessage(), e);
        }

        return renderType;
    }

    public static boolean shouldSkipTexture(ResourceLocation texture) {
        String path = texture.getPath();

        if (path.contains("armor") && (path.contains("leather") || path.contains("chain") ||
                path.contains("iron") || path.contains("gold") || path.contains("diamond") ||
                path.contains("netherite"))) {
            return true;
        }

        if (path.contains("/models/armor/")) return true;

        if (path.contains("/cape")) return true;
        if (path.endsWith("/cape.png")) return true;

        if (path.contains("glint")) return true;

        if (path.contains("/block/") || path.contains("/item/")) return true;

        if (path.contains("particle")) return true;

        if (path.contains("/environment/") || path.contains("/misc/")) return true;

        return texture.getNamespace().equals("visualhealth") && path.startsWith("dynamic/");
    }
}
