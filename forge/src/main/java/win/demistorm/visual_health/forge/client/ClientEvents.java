package win.demistorm.visual_health.forge.client;

import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import win.demistorm.visual_health.VisualHealth;
import win.demistorm.visual_health.client.DamageEventHandler;

// Client-side event handlers for Forge
// Registered with MOD event bus for entity events
@Mod.EventBusSubscriber(modid = VisualHealth.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ClientEvents {

    // Handle living entity damage events for weapon type tracking
    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent.Pre event) {
        LivingEntity entity = event.getEntity();

        // Only track on client side for visual wound system
        if (entity.level().isClientSide()) {
            DamageEventHandler.onLivingDamage(entity, event.getSource());
        }
        // Don't cancel the event - we're just tracking for visuals
    }
}
