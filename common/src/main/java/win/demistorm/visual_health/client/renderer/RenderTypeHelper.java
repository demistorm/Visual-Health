package win.demistorm.visual_health.client.renderer;

import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import win.demistorm.visual_health.VisualHealth;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class RenderTypeHelper {

    private static boolean vh$loggedUnwrap = false;

    private RenderTypeHelper() {
    }

    public static RenderType unwrapFully(RenderType renderType) {
        try {
            int depth = 0;
            while (depth < 8) {
                Method unwrap = renderType.getClass().getMethod("unwrap");
                RenderType unwrapped = (RenderType) unwrap.invoke(renderType);
                if (unwrapped == renderType || unwrapped == null) break;
                if (!vh$loggedUnwrap) {
                    vh$loggedUnwrap = true;
                }
                renderType = unwrapped;
                depth++;
            }
        } catch (NoSuchMethodException ignored) {
        } catch (Exception e) {
            VisualHealth.LOGGER.debug("VH unwrap error: {}", e.getMessage());
        }
        return renderType;
    }

    private static List<WrapperLayer> collectWrapperLayers(RenderType renderType) {
        List<WrapperLayer> layers = new ArrayList<>();

        try {
            int depth = 0;
            while (depth < 8) {
                Method unwrap;
                try {
                    unwrap = renderType.getClass().getMethod("unwrap");
                } catch (NoSuchMethodException e) {
                    break;
                }

                RenderType unwrapped = (RenderType) unwrap.invoke(renderType);
                if (unwrapped == renderType || unwrapped == null) break;

                String name = renderType.name;
                RenderStateShard extraShard = findExtraShard(renderType);

                layers.add(new WrapperLayer(renderType.getClass(), name, extraShard));
                renderType = unwrapped;
                depth++;
            }
        } catch (Exception ignored) {}

        return layers;
    }

    private static RenderStateShard findExtraShard(RenderType wrapper) {
        for (Field f : wrapper.getClass().getDeclaredFields()) {
            if (f.getType() == RenderStateShard.class) {
                try {
                    f.setAccessible(true);
                    return (RenderStateShard) f.get(wrapper);
                } catch (Exception ignored) {}
            }
        }
        return null;
    }

    private static RenderType rewrap(RenderType inner, List<WrapperLayer> layers) {
        if (layers.isEmpty()) return inner;

        RenderType current = inner;
        for (int i = layers.size() - 1; i >= 0; i--) {
            WrapperLayer layer = layers.get(i);
            if (layer.extra == null) {
                VisualHealth.LOGGER.debug("VH rewrap: no extra shard for layer {}, skipping rewrap", layer.name);
                return inner;
            }
            try {
                Constructor<? extends RenderType> ctor = layer.wrapperClass
                        .getConstructor(String.class, RenderType.class, RenderStateShard.class);
                current = ctor.newInstance(layer.name, current, layer.extra);
            } catch (Exception e) {
                VisualHealth.LOGGER.debug("VH rewrap failed for {}: {}", layer.name, e.getMessage());
                return inner;
            }
        }
        return current;
    }

    @Nullable
    public static ResourceLocation extractTexture(RenderType renderType) {
        RenderType unwrapped = unwrapFully(renderType);

        if (!(unwrapped instanceof RenderType.CompositeRenderType composite)) {
            return null;
        }

        RenderType.CompositeState state = composite.state();
        RenderStateShard.EmptyTextureStateShard texState = state.textureState;

        if (texState instanceof RenderStateShard.TextureStateShard tss) {
            return tss.texture.orElse(null);
        }

        if (texState instanceof RenderStateShard.MultiTextureStateShard mtss) {
            return mtss.cutoutTexture().orElse(null);
        }

        return null;
    }

    public static RenderType createWithTexture(RenderType original, ResourceLocation originalTexture, ResourceLocation newTexture) {
        List<WrapperLayer> layers = collectWrapperLayers(original);
        RenderType inner = unwrapFully(original);

        if (!(inner instanceof RenderType.CompositeRenderType composite)) {
            VisualHealth.LOGGER.debug("VH createWithTexture: unwrapped type is not CompositeRenderType, falling back");
            return inner;
        }

        RenderType.CompositeState origState = composite.state();

        RenderStateShard.EmptyTextureStateShard newTexState;
        if (origState.textureState instanceof RenderStateShard.TextureStateShard tss) {
            newTexState = new EtfAwareTextureStateShard(originalTexture, newTexture, tss.blur, tss.mipmap);
        } else {
            newTexState = new EtfAwareTextureStateShard(originalTexture, newTexture, false, false);
        }

        RenderType.CompositeState newState = RenderType.CompositeState.builder()
                .setTextureState(newTexState)
                .setShaderState(origState.shaderState)
                .setTransparencyState(origState.transparencyState)
                .setDepthTestState(origState.depthTestState)
                .setCullState(origState.cullState)
                .setLightmapState(origState.lightmapState)
                .setOverlayState(origState.overlayState)
                .setLayeringState(origState.layeringState)
                .setOutputState(origState.outputState)
                .setTexturingState(origState.texturingState)
                .setWriteMaskState(origState.writeMaskState)
                .setLineState(origState.lineState)
                .setColorLogicState(origState.colorLogicState)
                .createCompositeState(origState.outlineProperty);

        String name = composite.name;
        VertexFormat format = composite.format();
        VertexFormat.Mode mode = composite.mode();
        int bufferSize = composite.bufferSize();
        boolean affectsCrumbling = composite.affectsCrumbling();
        boolean sortOnUpload = shouldSortOnUpload(composite);

        RenderType.CompositeRenderType replacement =
                new RenderType.CompositeRenderType(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, newState);

        return rewrap(replacement, layers);
    }

    private static boolean shouldSortOnUpload(RenderType type) {
        try {
            for (Field f : RenderType.class.getDeclaredFields()) {
                if (f.getType() == boolean.class) {
                    f.setAccessible(true);
                    String fname = f.getName();
                    if (fname.contains("sort") || fname.contains("upload")) {
                        return f.getBoolean(type);
                    }
                }
            }
        } catch (Exception ignored) {}
        return false;
    }

    public static boolean shouldSkipRenderType(RenderType renderType) {
        RenderType unwrapped = unwrapFully(renderType);

        if (!(unwrapped instanceof RenderType.CompositeRenderType)) {
            return true;
        }

        String name = unwrapped.name;

        return switch (name) {
            case "eyes", "entity_shadow", "beacon_beam", "energy_swirl",
                 "leash", "lightning", "armor_glint", "armor_entity_glint",
                 "glint_translucent", "glint", "glint_direct",
                 "entity_glint", "entity_glint_direct" -> true;
            default -> false;
        };
    }

    private record WrapperLayer(Class<? extends RenderType> wrapperClass, String name, RenderStateShard extra) {}

    private static class EtfAwareTextureStateShard extends RenderStateShard.TextureStateShard {
        private final Optional<ResourceLocation> originalTexture;

        EtfAwareTextureStateShard(ResourceLocation original, ResourceLocation composited, boolean blur, boolean mipmap) {
            super(composited, blur, mipmap);
            this.originalTexture = Optional.of(original);
        }

        @Override
        public Optional<ResourceLocation> cutoutTexture() {
            return originalTexture;
        }
    }
}
