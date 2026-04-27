package win.demistorm.visual_health.client.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import win.demistorm.visual_health.VisualHealth;
import win.demistorm.visual_health.client.compat.PhysicsModBridge;
import win.demistorm.visual_health.client.texture.WoundTextureGenerator;

@Mixin(targets = "net.diebuddies.opengl.TextureHelper")
public class PhysicsTextureHelperMixin {

    @Overwrite
    public static int getLoadedTextures() {
        LivingEntity entity = PhysicsModBridge.getCapturingEntity();
        if (entity != null) {
            Integer glId = WoundTextureGenerator.getCompositedTextureGLId(entity);
            if (glId != null) {
                VisualHealth.LOGGER.debug("VH Physics: substituted composited texture GL ID {} for {}",
                        glId, entity.getName().getString());
                return glId;
            }
        }
        return RenderSystem.getShaderTexture(0);
    }
}
