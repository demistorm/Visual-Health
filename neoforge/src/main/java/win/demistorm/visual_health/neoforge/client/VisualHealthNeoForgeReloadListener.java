package win.demistorm.visual_health.neoforge.client;

import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;
import win.demistorm.visual_health.client.VisualHealthClient;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ReloadableResourceManager;

@EventBusSubscriber(modid = "visual_health", bus = Bus.MOD, value = Dist.CLIENT)
public class ClientHandler {

    @SubscribeEvent
    public static void addReloadListeners(AddClientReloadListenersEvent event) {
        // Must provide an ID for ordering/dependencies
        Identifier listenerId = Identifier.fromNamespaceAndPath
                ("yourmodid", "your_client_listener");
        event.addListener(listenerId, new YourCustomClientReloadListener());
    }
}