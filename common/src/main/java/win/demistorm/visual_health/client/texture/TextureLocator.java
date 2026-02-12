package win.demistorm.visual_health.client.texture;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import win.demistorm.visual_health.VisualHealth;

public class TextureLocator {

    private TextureLocator() {
        // Utility class - no instances
    }

    public static Identifier getEntityTexture(LivingEntity entity) {
        try {
            Minecraft client = Minecraft.getInstance();
            EntityRenderDispatcher dispatcher = client.getEntityRenderDispatcher();

            EntityRenderer<?, ?> baseRenderer = dispatcher.getRenderer(entity);

            if (!(baseRenderer instanceof LivingEntityRenderer<?, ?, ?> livingRenderer)) {
                if (VisualHealth.debugMode) {
                    VisualHealth.LOGGER.debug("Renderer is not LivingEntityRenderer for {}",
                            entity.getName().getString());
                }
                return null;
            }

            return resolveTexture(livingRenderer, entity);

        } catch (Exception e) {
            VisualHealth.LOGGER.error("Failed to get entity texture for {}: {}",
                    entity.getName().getString(), e.getMessage());
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private static <T extends LivingEntity, S extends LivingEntityRenderState>
    Identifier resolveTexture(LivingEntityRenderer<T, S, ?> renderer, LivingEntity entity) {
        try {
            T castEntity = (T) entity;
            S state = renderer.createRenderState();
            renderer.extractRenderState(castEntity, state, 0.0F);

            Identifier texture = renderer.getTextureLocation(state);

            if (VisualHealth.debugMode) {
                VisualHealth.LOGGER.debug("Got texture {} for {} (ID: {})",
                        texture, entity.getName().getString(), entity.getId());
            }

            return texture;

        } catch (NoSuchMethodError | NoClassDefFoundError e) {
            if (VisualHealth.debugMode) {
                VisualHealth.LOGGER.debug("Renderer does not support getTextureLocation(state) for {}: {}",
                        entity.getName().getString(), e.getMessage());
            }
            return null;
        } catch (Exception e) {
            if (VisualHealth.debugMode) {
                VisualHealth.LOGGER.debug("Failed to get texture for {}: {}",
                        entity.getName().getString(), e.getMessage());
            }
            return null;
        }
    }
}
