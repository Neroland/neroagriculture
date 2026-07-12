package za.co.neroland.neroagriculture.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import za.co.neroland.neroagriculture.NeroAgricultureCommon;

/** Authoritative block position for one newly opened client machine menu. */
public record MachineMenuPositionPayload(int containerId, long position) implements CustomPacketPayload {
    public static final Type<MachineMenuPositionPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(
            NeroAgricultureCommon.MOD_ID, "machine_menu_position"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MachineMenuPositionPayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.VAR_INT, MachineMenuPositionPayload::containerId,
                    ByteBufCodecs.VAR_LONG, MachineMenuPositionPayload::position,
                    MachineMenuPositionPayload::new);
    public BlockPos blockPos() { return BlockPos.of(position); }
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
