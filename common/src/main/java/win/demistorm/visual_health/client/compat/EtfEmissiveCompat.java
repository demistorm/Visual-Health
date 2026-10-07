package win.demistorm.visual_health.client.compat;

import win.demistorm.visual_health.VisualHealth;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

// Fixes emissive issues with ETF on 1.21.11+
public final class EtfEmissiveCompat {

    private static final String ETF_MANAGER_CLASS = "traben.entity_texture_features.features.ETFManager";

    private static Method getInstanceMethod;
    private static Field textureCacheField;
    private static boolean initialized = false;
    private static boolean broken = true;

    private EtfEmissiveCompat() {
    }

    public static void linkComposite(Object originalTexture, Object compositeTexture) {
        if (originalTexture == null || compositeTexture == null || !ensureInitialized()) return;

        try {
            Object etfManager = getInstanceMethod.invoke(null);
            Map<Object, Object> cache = asMap(textureCacheField.get(etfManager));
            if (cache == null) return;

            Object etfTexture = cache.get(originalTexture);
            if (etfTexture == null || cache.containsKey(compositeTexture)) return;

            cache.put(compositeTexture, etfTexture);

            if (VisualHealth.debugMode) {
                VisualHealth.LOGGER.debug("Linked composite texture {} to ETF texture data of {}",
                        compositeTexture, originalTexture);
            }
        } catch (Exception e) {
            broken = true;
            VisualHealth.LOGGER.warn("Failed to link composite texture to ETF texture cache, " +
                    "ETF emissives may stop rendering on damaged entities: {}", e.getMessage());
        }
    }

    private static boolean ensureInitialized() {
        if (initialized) return !broken;
        initialized = true;

        try {
            Class<?> managerClass = Class.forName(ETF_MANAGER_CLASS);
            getInstanceMethod = managerClass.getMethod("getInstance");
            textureCacheField = managerClass.getField("ETF_TEXTURE_CACHE");
            broken = false;
            VisualHealth.LOGGER.info("ETF detected, emissive texture linking enabled");
        } catch (ClassNotFoundException notInstalled) {
        } catch (Exception e) {
            VisualHealth.LOGGER.warn("ETF detected but emissive texture linking is unavailable: {}", e.getMessage());
        }
        return !broken;
    }

    @SuppressWarnings("unchecked")
    private static Map<Object, Object> asMap(Object map) {
        return map instanceof Map ? (Map<Object, Object>) map : null;
    }
}
