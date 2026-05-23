package win.demistorm.visual_health.client.compat;

import com.mojang.blaze3d.textures.GpuTextureView;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import win.demistorm.visual_health.VisualHealth;
import win.demistorm.visual_health.client.damagestate.EntityHealthTracker;
import win.demistorm.visual_health.client.texture.TextureLocator;
import win.demistorm.visual_health.client.texture.WoundTextureGenerator;

public final class PhysicsModBridge {

    private PhysicsModBridge() {}

    private static final ThreadLocal<LivingEntity> CAPTURING_ENTITY = new ThreadLocal<>();

    public static void setCapturingEntity(LivingEntity entity) {
        CAPTURING_ENTITY.set(entity);
    }

    public static LivingEntity getCapturingEntity() {
        return CAPTURING_ENTITY.get();
    }

    public static void clearCapturingEntity() {
        CAPTURING_ENTITY.remove();
    }

    public static GpuTextureView getCompositedTextureView(LivingEntity entity) {
        int tier = EntityHealthTracker.getDamageTier(entity.getId());
        if (tier <= 0) return null;

        ResourceLocation baseTexture = TextureLocator.getEntityTexture(entity);
        if (baseTexture == null) return null;

        ResourceLocation composited = WoundTextureGenerator.builder()
                .category("composite")
                .entity(entity)
                .damageTier(tier)
                .texture(baseTexture)
                .composite()
                .generate();
        if (composited == null) return null;

        try {
            return Minecraft.getInstance().getTextureManager()
                    .getTexture(composited).getTextureView();
        } catch (Exception e) {
            VisualHealth.LOGGER.debug("Failed to get texture view for composited texture: {}", e.getMessage());
        }
        return null;
    }
}
