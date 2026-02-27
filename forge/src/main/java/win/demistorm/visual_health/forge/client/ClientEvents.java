package win.demistorm.visual_health.forge.client;

import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import win.demistorm.visual_health.VisualHealth;
import win.demistorm.visual_health.client.damagestate.DamageEventHandler;

// Forge client-side event handlers
@Mod.EventBusSubscriber(modid = VisualHealth.MOD_ID, value = Dist.CLIENT)
public class ClientEvents {

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent event) {
        LivingEntity entity = event.getEntity();

        if (entity.getCommandSenderWorld().isClientSide) {
            DamageEventHandler.onLivingDamage(entity, event.getSource());
        }
    }
}
