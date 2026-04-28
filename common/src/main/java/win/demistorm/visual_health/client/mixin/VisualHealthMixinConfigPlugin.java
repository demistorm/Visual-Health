package win.demistorm.visual_health.client.mixin;

import org.objectweb.asm.tree.ClassNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class VisualHealthMixinConfigPlugin implements IMixinConfigPlugin {

    private static final Logger LOGGER = LoggerFactory.getLogger("VisualHealthMixin");

    private static final Map<String, String> COMPAT_NAMES = Map.of(
            "CompatBufferSourceMixin", "Iris/ImmediatelyFast",
            "PhysicsModCompatMixin", "Physics Mod",
            "PhysicsTextureHelperMixin", "Physics Mod"
    );

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
        String simpleName = mixinClassName.substring(mixinClassName.lastIndexOf('.') + 1);
        String compatName = COMPAT_NAMES.get(simpleName);
        if (compatName != null) {
            LOGGER.info("Compat mixin applied: {} -> {} (for {})", simpleName, targetClassName, compatName);
        }
    }

    @Override
    public void onLoad(String mixinPackage) {}

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return true;
    }
}
