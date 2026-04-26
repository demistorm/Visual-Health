package win.demistorm.visual_health.client.renderer;

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
                    System.err.println("VH UNWRAP: " + renderType.getClass().getName() + " -> " + unwrapped.getClass().getName());
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

                String name = ((RenderStateShard) renderType).name;
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

    public static RenderType createWithTexture(RenderType original, ResourceLocation newTexture) {
        List<WrapperLayer> layers = collectWrapperLayers(original);
        RenderType inner = unwrapFully(original);

        String name = ((RenderStateShard) inner).name;

        RenderType replacement = switch (name) {
            case "entity_solid" -> RenderType.entitySolid(newTexture);
            case "entity_cutout" -> RenderType.entityCutout(newTexture);
            case "entity_cutout_no_cull" -> RenderType.entityCutoutNoCull(newTexture);
            case "entity_cutout_no_cull_z_offset" -> RenderType.entityCutoutNoCullZOffset(newTexture);
            case "entity_translucent" -> RenderType.entityTranslucent(newTexture);
            case "entity_translucent_cull" -> RenderType.entityTranslucentCull(newTexture);
            case "entity_translucent_emissive" -> RenderType.entityTranslucentEmissive(newTexture);
            case "entity_smooth_cutout" -> RenderType.entitySmoothCutout(newTexture);
            case "armor_cutout_no_cull" -> RenderType.armorCutoutNoCull(newTexture);
            case "item_entity_translucent_cull" -> RenderType.itemEntityTranslucentCull(newTexture);
            case "entity_decal" -> RenderType.entityDecal(newTexture);
            case "entity_no_outline" -> RenderType.entityNoOutline(newTexture);
            case "eyes" -> RenderType.eyes(newTexture);
            case "entity_alpha" -> RenderType.entityCutoutNoCull(newTexture);
            default -> {
                VisualHealth.LOGGER.debug("Unknown render type name '{}', falling back to entityCutoutNoCull for {}", name, newTexture);
                yield RenderType.entityCutoutNoCull(newTexture);
            }
        };

        return rewrap(replacement, layers);
    }

    public static boolean shouldSkipRenderType(RenderType renderType) {
        RenderType unwrapped = unwrapFully(renderType);

        if (!(unwrapped instanceof RenderType.CompositeRenderType)) {
            return true;
        }

        String name = ((RenderStateShard) unwrapped).name;

        return switch (name) {
            case "eyes", "entity_shadow", "beacon_beam", "energy_swirl",
                 "leash", "lightning", "armor_glint", "armor_entity_glint",
                 "glint_translucent", "glint", "glint_direct",
                 "entity_glint", "entity_glint_direct" -> true;
            default -> false;
        };
    }

    private record WrapperLayer(Class<? extends RenderType> wrapperClass, String name, RenderStateShard extra) {}
}
