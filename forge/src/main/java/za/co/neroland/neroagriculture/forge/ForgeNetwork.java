package za.co.neroland.neroagriculture.forge;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.Channel;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.payload.PayloadFlow;

import za.co.neroland.neroagriculture.NeroAgricultureCommon;
import za.co.neroland.neroagriculture.network.AgricultureNetwork;
import za.co.neroland.neroagriculture.platform.NetworkPlatform;

public final class ForgeNetwork implements NetworkPlatform {
    private static Channel<CustomPacketPayload> channel;
    public static void register() {
        PayloadFlow<RegistryFriendlyByteBuf, CustomPacketPayload> play = ChannelBuilder.named(
                Identifier.fromNamespaceAndPath(NeroAgricultureCommon.MOD_ID, "main")).optional()
                .payloadChannel().play().bidirectional();
        for (AgricultureNetwork.Serverbound<?> payload : AgricultureNetwork.serverbound()) register(play, payload);
        channel = play.build();
    }
    private static <T extends CustomPacketPayload> void register(PayloadFlow<RegistryFriendlyByteBuf, CustomPacketPayload> play,
            AgricultureNetwork.Serverbound<T> payload) {
        play.addMain(payload.type(), codec(payload.codec()), (value, context) -> {
            if (context.getSender() instanceof ServerPlayer player) payload.handler().accept(value, player);
        });
    }
    @SuppressWarnings("unchecked") private static <T extends CustomPacketPayload> StreamCodec<RegistryFriendlyByteBuf, T> codec(
            StreamCodec<? super RegistryFriendlyByteBuf, T> codec) { return (StreamCodec<RegistryFriendlyByteBuf, T>) codec; }
    @Override public void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) { if (channel != null) channel.send(payload, PacketDistributor.PLAYER.with(player)); }
    @Override public void sendToServer(CustomPacketPayload payload) { if (channel != null) channel.send(payload, PacketDistributor.SERVER.noArg()); }
}
