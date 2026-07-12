package za.co.neroland.neroagriculture.platform;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

public interface NetworkPlatform {
    void sendToPlayer(ServerPlayer player, CustomPacketPayload payload);
    void sendToServer(CustomPacketPayload payload);
}
