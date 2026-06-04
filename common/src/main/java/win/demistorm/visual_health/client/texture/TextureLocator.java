package win.demistorm.visual_health.client.texture;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import win.demistorm.visual_health.VisualHealth;
import win.demistorm.visual_health.client.damagestate.EntityHealthTracker;

public class TextureLocator {

    private TextureLocator() {
    }

    public static Identifier getEntityTexture(LivingEntity entity) {
        Identifier cached = EntityHealthTracker.getCurrentRenderTexture();
        if (cached != null) {
            return cached;
        }

        return getEntityTextureFromRenderer(entity);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Identifier getEntityTextureFromRenderer(LivingEntity entity) {
        try {
            Minecraft client = Minecraft.getInstance();
            EntityRenderDispatcher dispatcher = client.getEntityRenderDispatcher();

            EntityRenderer renderer = dispatcher.getRenderer(entity);

            if (!(renderer instanceof LivingEntityRenderer livingRenderer)) {
                VisualHealth.LOGGER.debug("Renderer is not LivingEntityRenderer for {}",
                        entity.getName().getString());
                return null;
            }

            LivingEntityRenderState state = (LivingEntityRenderState) livingRenderer.createRenderState(entity, 0.0f);
            Identifier texture = livingRenderer.getTextureLocation(state);

            if (texture.getPath().startsWith("textures/atlas/")) {
                VisualHealth.LOGGER.debug("Skipping atlas texture {} for {} (ID: {})",
                        texture, entity.getName().getString(), entity.getId());
                return null;
            }

            return texture;

        } catch (Exception e) {
            VisualHealth.LOGGER.error("Failed to get entity texture for {}: {}",
                    entity.getName().getString(), e.getMessage());
            return null;
        }
    }
}
