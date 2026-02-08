package win.demistorm.visual_health.client.mixin;

import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import traben.entity_model_features.models.animation.EMFAnimationEntityContext;
import win.demistorm.visual_health.VisualHealth;
import win.demistorm.visual_health.client.EMFPerEntityTextures;

/**
 * Intercepts EMF model part rendering to apply per-entity wound textures.
 * This works around EMF's per-type model sharing by swapping textures at render time.
 * EMF shares EMFModelPart instances across all entities of the same type.
 * To apply different wound textures to different entities, we temporarily swap
 * the textureOverride field during each entity's render call.
 */
@Mixin(targets = "traben.entity_model_features.models.parts.EMFModelPart", remap = false)
public class EMFModelPartRenderMixin {

    // ThreadLocal to store the original texture that needs to be restored after rendering
    @Unique
    private static final ThreadLocal<Identifier> ORIGINAL_TEXTURE = new ThreadLocal<>();

    /**
     * Before EMF renders a model part with texture override, check if this entity has a wound texture.
     * If so, temporarily swap the textureOverride field for this render call only.
     */
    @Inject(
            method = "renderWithTextureOverride",
            at = @At("HEAD"),
            remap = false,
            require = 0
    )
    private void beforeRenderWithTexture(CallbackInfo ci) {
        try {
            // Get the current entity render state from EMF's context
            var emfState = EMFAnimationEntityContext.getEmfState();

            if (emfState == null) {
                return; // No entity context, nothing to do
            }

            // Get the actual entity object from EMF's state
            var emfEntity = emfState.emfEntity();
            if (emfEntity == null) {
                return;
            }

            // Cast to Entity to get Minecraft's entity ID
            if (!(emfEntity instanceof net.minecraft.world.entity.Entity entity)) {
                return;
            }

            int entityId = entity.getId();
            String entityKey = String.valueOf(entityId);

            // Check if this entity has a wound texture registered
            Identifier woundTexture = EMFPerEntityTextures.getWoundTextureById(entityKey);

            if (woundTexture == null) {
                return; // No wound texture for this entity
            }

            // Get the current texture override using accessor
            Identifier originalOverride = ((EMFModelPartAccessor) this).getTextureOverride();

            // Only swap if there's an override and it's different from our wound texture
            if (originalOverride != null && !originalOverride.equals(woundTexture)) {
                // Store the original texture so we can restore it after rendering
                ORIGINAL_TEXTURE.set(originalOverride);

                // Replace with wound texture for this render call
                ((EMFModelPartAccessor) this).setTextureOverride(woundTexture);

                if (VisualHealth.debugMode) {
                    VisualHealth.LOGGER.debug("Swapped EMF texture {} -> {} for entity ID {}",
                            originalOverride, woundTexture, entityId);
                }
            }
        } catch (Exception e) {
            VisualHealth.LOGGER.error("Error in EMF texture override (beforeRender)", e);
        }
    }

    /**
     * After rendering with texture override, restore the original texture.
     * This is critical to prevent affecting other entities of the same type.
     */
    @Inject(
            method = "renderWithTextureOverride",
            at = @At("RETURN"),
            remap = false,
            require = 0
    )
    private void afterRenderWithTexture(CallbackInfo ci) {
        try {
            // Restore the original texture if we swapped it
            Identifier originalTexture = ORIGINAL_TEXTURE.get();

            if (originalTexture != null) {
                ((EMFModelPartAccessor) this).setTextureOverride(originalTexture);

                // Clear the ThreadLocal to prevent memory leaks
                ORIGINAL_TEXTURE.remove();

                if (VisualHealth.debugMode) {
                    VisualHealth.LOGGER.debug("Restored EMF texture to {}", originalTexture);
                }
            }
        } catch (Exception e) {
            VisualHealth.LOGGER.error("Error restoring EMF texture (afterRender)", e);
            // Clear ThreadLocal even on error to prevent memory leaks
            ORIGINAL_TEXTURE.remove();
        }
    }
}
