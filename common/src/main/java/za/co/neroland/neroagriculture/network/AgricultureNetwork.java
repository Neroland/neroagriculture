package za.co.neroland.neroagriculture.network;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

import za.co.neroland.neroagriculture.machine.FoundationMachineBlockEntity;

/** Loader-neutral payload declarations and strict server-side request validation. */
public final class AgricultureNetwork {
    public record Clientbound<T extends CustomPacketPayload>(CustomPacketPayload.Type<T> type,
            StreamCodec<? super RegistryFriendlyByteBuf, T> codec, Consumer<T> handler) { }
    public record Serverbound<T extends CustomPacketPayload>(CustomPacketPayload.Type<T> type,
            StreamCodec<? super RegistryFriendlyByteBuf, T> codec, BiConsumer<T, ServerPlayer> handler) { }
    private static final List<Serverbound<?>> SERVERBOUND = new ArrayList<>();
    private static final List<Clientbound<?>> CLIENTBOUND = new ArrayList<>();

    private AgricultureNetwork() { }

    public static List<Serverbound<?>> serverbound() { return List.copyOf(SERVERBOUND); }
    public static List<Clientbound<?>> clientbound() { return List.copyOf(CLIENTBOUND); }

    public static <T extends CustomPacketPayload> void clientbound(CustomPacketPayload.Type<T> type,
            StreamCodec<? super RegistryFriendlyByteBuf, T> codec, Consumer<T> handler) {
        CLIENTBOUND.add(new Clientbound<>(type, codec, handler));
    }

    public static <T extends CustomPacketPayload> void serverbound(CustomPacketPayload.Type<T> type,
            StreamCodec<? super RegistryFriendlyByteBuf, T> codec, BiConsumer<T, ServerPlayer> handler) {
        SERVERBOUND.add(new Serverbound<>(type, codec, handler));
    }

    public static void init() {
        clientbound(MaterialCatalogSyncPayload.TYPE, MaterialCatalogSyncPayload.STREAM_CODEC,
                za.co.neroland.neroagriculture.catalog.ClientMaterialCatalog::accept);
        clientbound(MachineMenuPositionPayload.TYPE, MachineMenuPositionPayload.STREAM_CODEC,
                ClientMachineMenuPositions::accept);
        serverbound(MachineActionPayload.TYPE, MachineActionPayload.STREAM_CODEC, AgricultureNetwork::handleMachineAction);
    }

    public static void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        za.co.neroland.neroagriculture.platform.Services.NETWORK.sendToPlayer(player, payload);
    }

    private static void handleMachineAction(MachineActionPayload payload, ServerPlayer player) {
        // Direction and player identity are supplied by the serverbound loader handler. The fixed codec is
        // at most 30 bytes; action/value bounds, proximity, menu context and target type are checked here.
        if (payload.action() < 0 || payload.action() > 7 || payload.value() < 0 || payload.value() > 1_000) return;
        var pos = payload.blockPos();
        if (!player.level().isLoaded(pos) || player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5,
                pos.getZ() + 0.5) > 64.0) return;
        if (!(player.containerMenu instanceof za.co.neroland.neroagriculture.menu.FoundationMachineMenu menu)
                || !menu.blockPos().equals(pos)) return;
        if (!(player.level().getBlockEntity(pos) instanceof FoundationMachineBlockEntity machine)) return;
        if (payload.action() == 0) machine.tryResearch(player);
    }
}
