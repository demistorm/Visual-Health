package win.demistorm.visual_health.forge.client;

import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import win.demistorm.visual_health.VisualHealth;
import win.demistorm.visual_health.client.command.SaveDamageCommand;
import win.demistorm.visual_health.client.damagestate.DamageEventHandler;
import win.demistorm.visual_health.client.DamageEventHandler;

// Forge client-side event handlers
@Mod.EventBusSubscriber(modid = VisualHealth.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ClientEvents {

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent.Pre event) {
        LivingEntity entity = event.getEntity();

        if (entity.level().isClientSide()) {
            DamageEventHandler.onLivingDamage(entity, event.getSource());
        }
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        SaveDamageCommand.register(event.getDispatcher());
    }
}
