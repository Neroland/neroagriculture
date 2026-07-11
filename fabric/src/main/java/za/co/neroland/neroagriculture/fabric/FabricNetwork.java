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
        for (AgricultureNetwork.Clientbound<?> payload : AgricultureNetwork.clientbound()) registerType(payload);
        for (AgricultureNetwork.Serverbound<?> payload : AgricultureNetwork.serverbound()) register(payload);
    }

    public static void registerClient() {
        for (AgricultureNetwork.Clientbound<?> payload : AgricultureNetwork.clientbound()) registerClient(payload);
    }

    private static <T extends CustomPacketPayload> void registerType(AgricultureNetwork.Clientbound<T> payload) {
        PayloadTypeRegistry.clientboundPlay().register(payload.type(), payload.codec());
    }

    private static <T extends CustomPacketPayload> void registerClient(AgricultureNetwork.Clientbound<T> payload) {
        ClientPlayNetworking.registerGlobalReceiver(payload.type(), (value, context) ->
                context.client().execute(() -> payload.handler().accept(value)));
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
