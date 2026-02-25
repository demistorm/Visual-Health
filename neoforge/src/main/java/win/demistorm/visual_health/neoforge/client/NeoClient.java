package win.demistorm.visual_health.neoforge.client;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import win.demistorm.visual_health.VisualHealth;
import win.demistorm.visual_health.client.command.SaveDamageCommand;
import win.demistorm.visual_health.client.VisualHealthClient;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

// NeoForge client initialization
@EventBusSubscriber(modid = VisualHealth.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public class NeoClient {

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(NeoClient::onClientSetup);
        modEventBus.addListener(NeoClient::onRegisterReloadListeners);

        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent event) -> {
            SaveDamageCommand.register(event.getDispatcher());
        });
    }

    private static void onClientSetup(FMLClientSetupEvent event) {
        VisualHealth.LOGGER.info("NeoForge client initialization starting");

        VisualHealthClient.initializeClient();
        VisualHealthClient.registerDamageLayers();

        VisualHealth.LOGGER.info("NeoForge client initialization complete");
    }

    private static void onRegisterReloadListeners(RegisterClientReloadListenersEvent event) {
        VisualHealth.LOGGER.info("Registering Visual Health reload listener");

        ResourceLocation listenerId = ResourceLocation.fromNamespaceAndPath(
                VisualHealth.MOD_ID,
                "texture_reload"
        );

        event.registerReloadListener(new PreparableReloadListener() {
            @Override
            public CompletableFuture<Void> reload(PreparationBarrier preparationBarrier, ResourceManager resourceManager, ProfilerFiller prepareProfiler, ProfilerFiller applyProfiler, Executor backgroundExecutor, Executor gameExecutor) {
                return preparationBarrier.wait(null).thenRunAsync(() -> {
                    VisualHealth.LOGGER.info("Resource reload triggered - refreshing damage textures");
                    VisualHealthClient.onResourcesReloaded();
                }, gameExecutor);
            }
        });
    }
}
