package win.demistorm.visual_health.client.renderer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import win.demistorm.visual_health.ConfigHelper;
import win.demistorm.visual_health.VisualHealth;
import win.demistorm.visual_health.client.DamageRenderCheck;
import win.demistorm.visual_health.client.damagestate.EntityHealthTracker;
import win.demistorm.visual_health.client.texture.TextureLocator;
import win.demistorm.visual_health.client.texture.WoundTextureGenerator;

public final class TextureSwapHelper {

    private static final int RENDER_DISTANCE_SQ = 96 * 96;

    private TextureSwapHelper() {}

    public static RenderType swapTexture(RenderType renderType) {
        LivingEntity entity = EntityHealthTracker.getCurrentRenderEntity();
        if (entity == null) return null;

        if (!DamageRenderCheck.shouldRender(entity, DamageRenderCheck.ALL)) {
            return null;
        }

        int damageTier = EntityHealthTracker.getDamageTier(entity.getId());
        if (damageTier == 0) return null;

        Identifier baseTexture = TextureLocator.getEntityTexture(entity);
        if (baseTexture == null) {
            VisualHealth.LOGGER.debug("VH swap: no base texture for {}", entity.getName().getString());
            return null;
        }

        if (shouldSkipTexture(baseTexture)) return null;

        if (!ConfigHelper.INSTANCE.drawOnOptifineEmissives && baseTexture.getPath().endsWith("_e.png")) {
            return null;
        }

        if (entity.distanceToSqr(Minecraft.getInstance().gameRenderer.getMainCamera().position())
                > RENDER_DISTANCE_SQ) {
            return null;
        }

        try {
            Identifier composited = WoundTextureGenerator.builder()
                    .category("composite")
                    .entity(entity)
                    .damageTier(damageTier)
                    .texture(baseTexture)
                    .composite()
                    .generate();

            if (composited != null) {
                VisualHealth.LOGGER.debug("Swapped texture {} -> {} for {} (ID: {})",
                        baseTexture, composited, entity.getName().getString(), entity.getId());
                return RenderTypeHelper.createWithTexture(renderType, baseTexture, composited);
            }
        } catch (Exception e) {
            VisualHealth.LOGGER.error("Failed to swap texture for {}: {}",
                    entity.getName().getString(), e.getMessage(), e);
        }

        return null;
    }

    public static boolean shouldSkipTexture(Identifier texture) {
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
