package win.demistorm.visual_health.client.renderer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.npc.AbstractVillager;
import win.demistorm.visual_health.ConfigHelper;
import win.demistorm.visual_health.VisualHealth;
import win.demistorm.visual_health.client.damagestate.EntityHealthTracker;
import win.demistorm.visual_health.client.texture.WoundTextureGenerator;

public final class BufferSourceSwapHelper {

    private static final int RENDER_DISTANCE_SQ = 96 * 96;
    private static int debugLogCounter = 0;

    private BufferSourceSwapHelper() {}

    public static RenderType swapTexture(RenderType renderType) {
        LivingEntity entity = EntityHealthTracker.getCurrentRenderEntity();
        if (entity == null) {
            // if (debugLogCounter < 5) VisualHealth.LOGGER.debug("VH swap: entity is null");
            return renderType;
        }

        if (entity.isInvisible()) {
            VisualHealth.LOGGER.debug("VH swap: {} is invisible, skipping", entity.getName().getString());
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

        if (!shouldSwapTexture(texture)) {
            VisualHealth.LOGGER.debug("VH swap: {} tier={} texture {} filtered by path",
                    entity.getName().getString(), damageTier, texture);
            return renderType;
        }

        if (entity.distanceToSqr(Minecraft.getInstance().gameRenderer.getMainCamera().getPosition())
                > RENDER_DISTANCE_SQ) {
            VisualHealth.LOGGER.debug("VH swap: {} too far away", entity.getName().getString());
            return renderType;
        }

        MobCategory category = entity.getType().getCategory();
        boolean isMonster = category == MobCategory.MONSTER;
        if (!isMonster && !ConfigHelper.INSTANCE.damagePassiveMobs) {
            VisualHealth.LOGGER.debug("VH swap: {} is passive mob and passive mobs disabled",
                    entity.getName().getString());
            return renderType;
        }

        if (entity instanceof AbstractVillager && !ConfigHelper.INSTANCE.damageVillagers) {
            VisualHealth.LOGGER.debug("VH swap: {} is villager and villager damage disabled",
                    entity.getName().getString());
            return renderType;
        }

        VisualHealth.LOGGER.debug("VH swap: attempting composite for {} tier={} texture={}",
                entity.getName().getString(), damageTier, texture);

        try {
            ResourceLocation replacement = WoundTextureGenerator.generateCompositedTexture(
                    entity, damageTier, texture);

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

    static boolean shouldSwapTexture(ResourceLocation texture) {
        String path = texture.getPath();

        if (path.contains("armor") && (path.contains("leather") || path.contains("chain") ||
                path.contains("iron") || path.contains("gold") || path.contains("diamond") ||
                path.contains("netherite"))) {
            return false;
        }

        if (path.contains("/models/armor/")) return false;

        if (path.contains("/cape")) return false;
        if (path.endsWith("/cape.png")) return false;

        if (path.contains("glint")) return false;

        if (path.contains("/block/") || path.contains("/item/")) return false;

        if (path.contains("particle")) return false;

        if (path.contains("/environment/") || path.contains("/misc/")) return false;

        if (texture.getNamespace().equals("visualhealth") && path.startsWith("dynamic/")) {
            return false;
        }

        return true;
    }
}
