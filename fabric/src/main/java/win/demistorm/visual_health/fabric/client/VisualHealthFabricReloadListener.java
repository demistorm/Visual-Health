package win.demistorm.visual_health.fabric.client;

import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import win.demistorm.visual_health.client.VisualHealthClient;

public class VisualHealthFabricReloadListener
        implements SimpleSynchronousResourceReloadListener {

    @Override
    public ResourceLocation getFabricId() {
        return new ResourceLocation("visualhealth", "client_reload");
    }

    @Override
    public void onResourceManagerReload(ResourceManager resourceManager) {
        VisualHealthClient.onResourcesReloaded();
    }
}
