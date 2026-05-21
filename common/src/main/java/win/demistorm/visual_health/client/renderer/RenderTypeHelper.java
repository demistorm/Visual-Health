package win.demistorm.visual_health.client.renderer;

import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;
import win.demistorm.visual_health.VisualHealth;

import java.util.Map;

public final class RenderTypeHelper {

    private RenderTypeHelper() {}

    @Nullable
    public static Identifier extractTexture(RenderType renderType) {
        try {
            RenderSetup setup = renderType.state;
            RenderSetup.TextureBinding binding = (RenderSetup.TextureBinding) setup.textures.get("Sampler0");
            return binding != null ? binding.location() : null;
        } catch (Exception e) {
            VisualHealth.LOGGER.debug("VH extractTexture: failed for {}: {}", renderType, e.getMessage());
            return null;
        }
    }

    public static RenderType createWithTexture(RenderType original, Identifier originalTexture, Identifier newTexture) {
        try {
            RenderSetup orig = original.state;

            RenderSetup.RenderSetupBuilder builder = RenderSetup.builder(original.pipeline())
                    .withTexture("Sampler0", newTexture);

            if (orig.useLightmap) builder.useLightmap();
            if (orig.useOverlay) builder.useOverlay();
            if (original.affectsCrumbling()) builder.affectsCrumbling();
            if (original.sortOnUpload()) builder.sortOnUpload();

            builder.bufferSize(original.bufferSize());
            builder.setLayeringTransform(orig.layeringTransform);
            builder.setOutputTarget(orig.outputTarget);
            builder.setTextureTransform(orig.textureTransform);
            builder.setOutline(orig.outlineProperty);

            for (Map.Entry<String, ?> entry : orig.textures.entrySet()) {
                if (!"Sampler0".equals(entry.getKey())) {
                    RenderSetup.TextureBinding binding = (RenderSetup.TextureBinding) entry.getValue();
                    builder.withTexture(entry.getKey(), binding.location(), binding.sampler());
                }
            }

            return RenderType.create(original.name, builder.createRenderSetup());
        } catch (Exception e) {
            VisualHealth.LOGGER.error("VH createWithTexture: failed, falling back to entityCutoutNoCull: {}", e.getMessage());
            return RenderTypes.entityCutoutNoCull(newTexture);
        }
    }
}
