package win.demistorm.visual_health.client.mixin;

import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import traben.entity_model_features.models.animation.EMFAnimationEntityContext;
import win.demistorm.visual_health.VisualHealth;
import win.demistorm.visual_health.client.EMFPerEntityTextures;

// Swap EMF textures per entity for wound rendering
@Pseudo
@Mixin(targets = "traben.entity_model_features.models.parts.EMFModelPart", remap = false)
public class EMFModelPartRenderMixin {

    @Unique
    private static final ThreadLocal<Identifier> ORIGINAL_TEXTURE = new ThreadLocal<>();
    @Inject(
            method = "renderWithTextureOverride",
            at = @At("HEAD"),
            remap = false,
            require = 0
    )
    private void beforeRenderWithTexture(CallbackInfo ci) {
        try {
            var emfState = EMFAnimationEntityContext.getEmfState();

            if (emfState == null) {
                return;
            }

            var emfEntity = emfState.emfEntity();
            if (emfEntity == null) {
                return;
            }

            if (!(emfEntity instanceof net.minecraft.world.entity.Entity entity)) {
                return;
            }

            int entityId = entity.getId();
            String entityKey = String.valueOf(entityId);

            Identifier woundTexture = EMFPerEntityTextures.getWoundTextureById(entityKey);

            if (woundTexture == null) {
                return;
            }

            Identifier originalOverride = ((EMFModelPartAccessor) this).getTextureOverride();

            if (originalOverride != null && !originalOverride.equals(woundTexture)) {
                ORIGINAL_TEXTURE.set(originalOverride);
                ((EMFModelPartAccessor) this).setTextureOverride(woundTexture);
            }
        } catch (Exception e) {
            VisualHealth.LOGGER.error("Error in EMF texture override (beforeRender)", e);
        }
    }

    // Restore original texture after rendering
    @Inject(
            method = "renderWithTextureOverride",
            at = @At("RETURN"),
            remap = false,
            require = 0
    )
    private void afterRenderWithTexture(CallbackInfo ci) {
        try {
            Identifier originalTexture = ORIGINAL_TEXTURE.get();

            if (originalTexture != null) {
                ((EMFModelPartAccessor) this).setTextureOverride(originalTexture);
                ORIGINAL_TEXTURE.remove();
            }
        } catch (Exception e) {
            VisualHealth.LOGGER.error("Error restoring EMF texture (afterRender)", e);
            ORIGINAL_TEXTURE.remove();
        }
    }
}
