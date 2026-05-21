package win.demistorm.visual_health.client.mixin;

import org.objectweb.asm.tree.ClassNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.service.MixinService;

import java.io.IOException;
import java.util.List;
import java.util.Set;

public class VisualHealthMixinConfigPlugin implements IMixinConfigPlugin {

    private static final Logger LOGGER = LoggerFactory.getLogger("VisualHealthMixin");

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
        if (simpleName.startsWith("Physics")) {
            LOGGER.info("Compat mixin applied: {} -> {} (for Physics Mod)", simpleName, targetClassName);
        }
    }

    @Override
    public void onLoad(String mixinPackage) {}

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (mixinClassName.startsWith("win.demistorm.visual_health.client.mixin.Physics")) {
            try {
                MixinService.getService().getBytecodeProvider().getClassNode(targetClassName);
            } catch (ClassNotFoundException | IOException e) {
                LOGGER.info("Skipping Physics Mod compat mixin '{}' (Physics Mod not installed)", mixinClassName);
                return false;
            }
        }
        return true;
    }
}
