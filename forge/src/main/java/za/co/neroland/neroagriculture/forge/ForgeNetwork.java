package za.co.neroland.neroagriculture.forge;

//? if >=1.20.5 {
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
        for (AgricultureNetwork.Clientbound<?> payload : AgricultureNetwork.clientbound()) register(play, payload);
        for (AgricultureNetwork.Serverbound<?> payload : AgricultureNetwork.serverbound()) register(play, payload);
        channel = play.build();
    }
    private static <T extends CustomPacketPayload> void register(PayloadFlow<RegistryFriendlyByteBuf, CustomPacketPayload> play,
            AgricultureNetwork.Clientbound<T> payload) {
        play.addMain(payload.type(), codec(payload.codec()), (value, context) -> payload.handler().accept(value));
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
//?} else {
/*import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
//? if >=1.20.2 {
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.SimpleChannel;
//?} else {
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
//?}

import za.co.neroland.neroagriculture.NeroAgricultureCommon;
import za.co.neroland.neroagriculture.network.AgricultureNetwork;
import za.co.neroland.nerolandcore.network.legacy.LegacyPayload;
import za.co.neroland.neroagriculture.platform.NetworkPlatform;

public final class ForgeNetwork implements NetworkPlatform {
    private static final int PROTOCOL_VERSION = 1;
    private static SimpleChannel channel;

    public static void register() {
        //? if >=1.20.2 {
        channel = ChannelBuilder.named(new ResourceLocation(NeroAgricultureCommon.MOD_ID, "main"))
                .networkProtocolVersion(PROTOCOL_VERSION).optional().simpleChannel();
        //?} else {
        channel = NetworkRegistry.newSimpleChannel(new ResourceLocation(NeroAgricultureCommon.MOD_ID, "main"),
                () -> Integer.toString(PROTOCOL_VERSION), ForgeNetwork::acceptVersion, ForgeNetwork::acceptVersion);
        //?}
        int id = 0;
        for (AgricultureNetwork.Clientbound<?> packet : AgricultureNetwork.clientbound()) {
            registerClientbound(packet, id++);
        }
        for (AgricultureNetwork.Serverbound<?> packet : AgricultureNetwork.serverbound()) {
            registerServerbound(packet, id++);
        }
    }

    //? if <1.20.2 {
    private static boolean acceptVersion(String version) {
        return Integer.toString(PROTOCOL_VERSION).equals(version) || NetworkRegistry.ABSENT.version().equals(version)
                || NetworkRegistry.ACCEPTVANILLA.equals(version);
    }
    //?}

    private static <T extends LegacyPayload> void registerClientbound(AgricultureNetwork.Clientbound<T> packet, int id) {
        channel.messageBuilder(packet.type().payloadClass(), id, NetworkDirection.PLAY_TO_CLIENT)
                .encoder((payload, buffer) -> packet.codec().encode(buffer, payload))
                .decoder(packet.codec()::decode)
                .consumerMainThread((payload, context) -> packet.handler().accept(payload))
                .add();
    }

    private static <T extends LegacyPayload> void registerServerbound(AgricultureNetwork.Serverbound<T> packet, int id) {
        channel.messageBuilder(packet.type().payloadClass(), id, NetworkDirection.PLAY_TO_SERVER)
                .encoder((payload, buffer) -> packet.codec().encode(buffer, payload))
                .decoder(packet.codec()::decode)
                .consumerMainThread((payload, context) -> {
                    //? if >=1.20.2 {
                    ServerPlayer player = context.getSender();
                    //?} else {
                    ServerPlayer player = context.get().getSender();
                    //?}
                    if (player != null) {
                        packet.handler().accept(payload, player);
                    }
                })
                .add();
    }

    @Override
    public void sendToPlayer(ServerPlayer player, LegacyPayload payload) {
        //? if >=1.20.2 {
        var connection = player.connection.getConnection();
        //?} else {
        var connection = player.connection.connection;
        //?}
        if (channel != null && channel.isRemotePresent(connection)) {
            //? if >=1.20.2 {
            channel.send(payload, connection);
            //?} else {
            channel.send(PacketDistributor.PLAYER.with(() -> player), payload);
            //?}
        }
    }

    @Override
    public void sendToServer(LegacyPayload payload) {
        var connection = Minecraft.getInstance().getConnection();
        if (channel != null && connection != null && channel.isRemotePresent(connection.getConnection())) {
            //? if >=1.20.2 {
            channel.send(payload, connection.getConnection());
            //?} else {
            channel.sendToServer(payload);
            //?}
        }
    }
}
*///?}
