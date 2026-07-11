package za.co.neroland.neroagriculture.network;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

import za.co.neroland.neroagriculture.machine.FoundationMachineBlockEntity;

/** Loader-neutral payload declarations and strict server-side request validation. */
public final class AgricultureNetwork {
    public record Serverbound<T extends CustomPacketPayload>(CustomPacketPayload.Type<T> type,
            StreamCodec<? super RegistryFriendlyByteBuf, T> codec, BiConsumer<T, ServerPlayer> handler) { }
    private static final List<Serverbound<?>> SERVERBOUND = new ArrayList<>();

    private AgricultureNetwork() { }

    public static List<Serverbound<?>> serverbound() { return List.copyOf(SERVERBOUND); }

    public static <T extends CustomPacketPayload> void serverbound(CustomPacketPayload.Type<T> type,
            StreamCodec<? super RegistryFriendlyByteBuf, T> codec, BiConsumer<T, ServerPlayer> handler) {
        SERVERBOUND.add(new Serverbound<>(type, codec, handler));
    }

    public static void init() {
        serverbound(MachineActionPayload.TYPE, MachineActionPayload.STREAM_CODEC, AgricultureNetwork::handleMachineAction);
    }

    private static void handleMachineAction(MachineActionPayload payload, ServerPlayer player) {
        // Direction and player identity are supplied by the serverbound loader handler. The fixed codec is
        // at most 30 bytes; action/value bounds, proximity, menu context and target type are checked here.
        if (payload.action() < 0 || payload.action() > 7 || payload.value() < 0 || payload.value() > 1_000) return;
        var pos = payload.blockPos();
        if (!player.level().isLoaded(pos) || player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5,
                pos.getZ() + 0.5) > 64.0) return;
        if (!(player.containerMenu instanceof za.co.neroland.neroagriculture.menu.FoundationMachineMenu)) return;
        if (!(player.level().getBlockEntity(pos) instanceof FoundationMachineBlockEntity machine)) return;
        // Stage 2 deliberately exposes only a bounded no-op action surface. Later machine stages map
        // validated action ids onto concrete controls without changing the wire contract.
        machine.setChanged();
    }
}
