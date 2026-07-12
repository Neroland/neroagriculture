package za.co.neroland.neroagriculture.neoforge;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import za.co.neroland.neroagriculture.network.AgricultureNetwork;
import za.co.neroland.neroagriculture.platform.NetworkPlatform;

public final class NeoForgeNetwork implements NetworkPlatform {
    public static void register(IEventBus bus) { bus.addListener(NeoForgeNetwork::onRegister); }
    private static void onRegister(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1").optional();
        for (AgricultureNetwork.Clientbound<?> payload : AgricultureNetwork.clientbound()) register(registrar, payload);
        for (AgricultureNetwork.Serverbound<?> payload : AgricultureNetwork.serverbound()) register(registrar, payload);
    }
    private static <T extends CustomPacketPayload> void register(PayloadRegistrar registrar,
            AgricultureNetwork.Clientbound<T> payload) {
        registrar.playToClient(payload.type(), payload.codec(),
                (value, context) -> context.enqueueWork(() -> payload.handler().accept(value)));
    }
    private static <T extends CustomPacketPayload> void register(PayloadRegistrar registrar,
            AgricultureNetwork.Serverbound<T> payload) {
        registrar.playToServer(payload.type(), payload.codec(), (value, context) -> context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) payload.handler().accept(value, player);
        }));
    }
    @Override public void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) { PacketDistributor.sendToPlayer(player, payload); }
    @Override public void sendToServer(CustomPacketPayload payload) { ClientPacketDistributor.sendToServer(payload); }
}
