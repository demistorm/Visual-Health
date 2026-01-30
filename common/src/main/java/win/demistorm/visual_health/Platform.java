package win.demistorm.visual_health;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

// Platform abstraction for loader-specific code
public interface Platform {

    // Send packet from client to server
    void sendToServer(RegistryFriendlyByteBuf packet);

    // Send packet from server to specific client
    void sendToPlayer(ServerPlayer player, RegistryFriendlyByteBuf packet);
}
