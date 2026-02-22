package win.demistorm.visual_health.client.mixin;

import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Accessor interface for EMFModelPart to provide clean access to textureOverride field.
 * Uses Mixin's @Accessor to generate getter/setter methods at compile time.
 */
@Pseudo
@Mixin(targets = "traben.entity_model_features.models.parts.EMFModelPart", remap = false)
public interface EMFModelPartAccessor {

    /**
     * Get the current texture override for this model part.
     *
     * @return The texture override identifier, or null if none
     */
    @Accessor("textureOverride")
    Identifier getTextureOverride();

    /**
     * Set the texture override for this model part.
     *
     * @param texture The new texture override identifier
     */
    @Accessor("textureOverride")
    void setTextureOverride(Identifier texture);
}
