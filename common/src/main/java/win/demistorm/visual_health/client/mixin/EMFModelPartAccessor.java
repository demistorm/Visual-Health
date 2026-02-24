package win.demistorm.visual_health.client.mixin;

import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Accessor;

// Access EMF's textureOverride field
@Pseudo
@Mixin(targets = "traben.entity_model_features.models.parts.EMFModelPart", remap = false)
public interface EMFModelPartAccessor {

    @Accessor("textureOverride")
    Identifier getTextureOverride();

    @Accessor("textureOverride")
    void setTextureOverride(Identifier texture);
}
