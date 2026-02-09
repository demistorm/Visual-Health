package win.demistorm.visual_health.neoforge.client;

import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.minecraft.world.entity.LivingEntity;
import win.demistorm.visual_health.VisualHealth;
import win.demistorm.visual_health.client.DamageEventHandler;
import win.demistorm.visual_health.client.VisualHealthClient;

// Client-side event handlers for NeoForge
// Registered manually to NeoForge EVENT_BUS (no @EventBusSubscriber)
public class ClientEvents {

    // Register client-side damage event handler
    // Called from VisualHealthNeoForge during initialization
    public static void register() {
        VisualHealth.LOGGER.info("Registering NeoForge client damage event handler");

        NeoForge.EVENT_BUS.addListener((LivingDamageEvent.Pre event) -> {
            LivingEntity entity = event.getEntity();

            // Only track on client side for visual wound system
            if (entity.level().isClientSide()) {
                DamageEventHandler.onLivingDamage(entity, event.getSource());
            }
            // Don't cancel the event - we're just tracking for visuals
        });
    }
}
