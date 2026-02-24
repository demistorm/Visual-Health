package win.demistorm.visual_health.neoforge.client;

import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.minecraft.world.entity.LivingEntity;
import win.demistorm.visual_health.VisualHealth;
import win.demistorm.visual_health.client.damagestate.DamageEventHandler;

// NeoForge client-side event handlers
public class ClientEvents {

    public static void register() {
        VisualHealth.LOGGER.info("Registering NeoForge client damage event handler");

        NeoForge.EVENT_BUS.addListener((LivingDamageEvent.Pre event) -> {
            LivingEntity entity = event.getEntity();

            if (entity.level().isClientSide()) {
                DamageEventHandler.onLivingDamage(entity, event.getSource());
            }
        });
    }
}
