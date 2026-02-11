package win.demistorm.visual_health.client;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import win.demistorm.visual_health.VisualHealth;
import win.demistorm.visual_health.client.texture.WoundTextureGenerator;

public final class SaveDamageCommand {

    private SaveDamageCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        if (!VisualHealth.debugMode) {
            return;
        }

        dispatcher.register(Commands.literal("saveDamage")
                .executes(context -> {
                    WoundTextureGenerator.saveAllCachedTextures();
                    context.getSource().sendSuccess(() -> Component.literal("Saved all wound textures to VHDamage directory"), true);
                    return 1;
                }));
    }
}
