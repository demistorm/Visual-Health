package win.demistorm.visual_health.client.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;
import win.demistorm.visual_health.ConfigHelper;
import win.demistorm.visual_health.VisualHealth;
import win.demistorm.visual_health.client.compat.PhysicsModBridge;
import win.demistorm.visual_health.client.texture.WoundTextureGenerator;

@Mixin(targets = "net.diebuddies.opengl.TextureHelper")
public class PhysicsTextureHelperMixin {

    @Unique
    private static boolean vh$fired = false;

    @Overwrite(remap = false)
    public static int getLoadedTextures() {
        if (!vh$fired) {
            vh$fired = true;
            VisualHealth.LOGGER.debug("VH COMPAT FIRED on PhysicsTextureHelper");
        }
        LivingEntity entity = PhysicsModBridge.getCapturingEntity();
        if (entity != null) {
            if (ConfigHelper.INSTANCE.playerDamageOnly && !(entity instanceof Player)) {
                return RenderSystem.getShaderTexture(0);
            }
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
