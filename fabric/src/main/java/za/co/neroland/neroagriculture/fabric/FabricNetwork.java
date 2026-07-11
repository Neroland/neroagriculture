package za.co.neroland.neroagriculture.fabric;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

import za.co.neroland.neroagriculture.network.AgricultureNetwork;
import za.co.neroland.neroagriculture.platform.NetworkPlatform;

public final class FabricNetwork implements NetworkPlatform {
    public static void register() {
        for (AgricultureNetwork.Serverbound<?> payload : AgricultureNetwork.serverbound()) register(payload);
    }

    private static <T extends CustomPacketPayload> void register(AgricultureNetwork.Serverbound<T> payload) {
        PayloadTypeRegistry.serverboundPlay().register(payload.type(), payload.codec());
        ServerPlayNetworking.registerGlobalReceiver(payload.type(), (value, context) -> {
            ServerPlayer player = context.player();
            player.level().getServer().execute(() -> payload.handler().accept(value, player));
        });
    }

    @Override public void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) { ServerPlayNetworking.send(player, payload); }
    @Override public void sendToServer(CustomPacketPayload payload) { ClientPlayNetworking.send(payload); }
}
