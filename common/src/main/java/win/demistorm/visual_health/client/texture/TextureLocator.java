package win.demistorm.visual_health.client.texture;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import win.demistorm.visual_health.VisualHealth;

public class TextureLocator {

    private TextureLocator() {
    }

    @SuppressWarnings("unchecked")
    public static ResourceLocation getEntityTexture(LivingEntity entity) {
        try {
            Minecraft client = Minecraft.getInstance();
            EntityRenderDispatcher dispatcher = client.getEntityRenderDispatcher();

            EntityRenderer<? super LivingEntity> baseRenderer = dispatcher.getRenderer(entity);

            if (!(baseRenderer instanceof LivingEntityRenderer)) {
                VisualHealth.LOGGER.debug("Renderer is not LivingEntityRenderer for {}",
                        entity.getName().getString());
                return null;
            }

            return resolveTexture((LivingEntityRenderer<LivingEntity, ?>) baseRenderer, entity);

        } catch (Exception e) {
            VisualHealth.LOGGER.error("Failed to get entity texture for {}: {}",
                    entity.getName().getString(), e.getMessage());
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private static <T extends LivingEntity, M extends EntityModel<T>>
    ResourceLocation resolveTexture(LivingEntityRenderer<T, M> renderer, LivingEntity entity) {
        try {
            T castEntity = (T) entity;
            ResourceLocation texture = renderer.getTextureLocation(castEntity);

            VisualHealth.LOGGER.debug("Got texture {} for {} (ID: {})",
                    texture, entity.getName().getString(), entity.getId());

            return texture;

        } catch (Exception e) {
            VisualHealth.LOGGER.debug("Failed to get texture for {}: {}",
                    entity.getName().getString(), e.getMessage());
            return null;
        }
    }
}
