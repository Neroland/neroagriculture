package za.co.neroland.neroagriculture.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
//? if >=1.20.5 {
import net.minecraft.network.codec.ByteBufCodecs;
//?}
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import za.co.neroland.neroagriculture.NeroAgricultureCommon;

/** Authoritative block position for one newly opened client machine menu. */
public record MachineMenuPositionPayload(int containerId, long position) implements CustomPacketPayload {
    //? if >=1.20.5 {
    public static final Type<MachineMenuPositionPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(
            NeroAgricultureCommon.MOD_ID, "machine_menu_position"));
    //?} else {
    /*public static final Type<MachineMenuPositionPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(
            NeroAgricultureCommon.MOD_ID, "machine_menu_position"), MachineMenuPositionPayload.class);
    *///?}
    //? if >=1.20.5 {
    public static final StreamCodec<RegistryFriendlyByteBuf, MachineMenuPositionPayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.VAR_INT, MachineMenuPositionPayload::containerId,
                    ByteBufCodecs.VAR_LONG, MachineMenuPositionPayload::position,
                    MachineMenuPositionPayload::new);
    //?} else {
    /*public static final StreamCodec<net.minecraft.network.FriendlyByteBuf, MachineMenuPositionPayload> STREAM_CODEC =
            StreamCodec.of((buffer, packet) -> {
                buffer.writeVarInt(packet.containerId());
                buffer.writeVarLong(packet.position());
            }, buffer -> new MachineMenuPositionPayload(buffer.readVarInt(), buffer.readVarLong()));
    *///?}
    public BlockPos blockPos() { return BlockPos.of(position); }
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
