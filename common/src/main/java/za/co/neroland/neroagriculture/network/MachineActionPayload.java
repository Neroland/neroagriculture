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

/** Fixed-size client request. Semantic validation remains server-side. */
public record MachineActionPayload(long position, int action, int value) implements CustomPacketPayload {
    //? if >=1.20.5 {
    public static final Type<MachineActionPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(
            NeroAgricultureCommon.MOD_ID, "machine_action"));
    //?} else {
    /*public static final Type<MachineActionPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(
            NeroAgricultureCommon.MOD_ID, "machine_action"), MachineActionPayload.class);
    *///?}
    //? if >=1.20.5 {
    public static final StreamCodec<RegistryFriendlyByteBuf, MachineActionPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_LONG, MachineActionPayload::position,
            ByteBufCodecs.VAR_INT, MachineActionPayload::action,
            ByteBufCodecs.VAR_INT, MachineActionPayload::value,
            MachineActionPayload::new);
    //?} else {
    /*public static final StreamCodec<net.minecraft.network.FriendlyByteBuf, MachineActionPayload> STREAM_CODEC =
            StreamCodec.of((buffer, packet) -> {
                buffer.writeVarLong(packet.position());
                buffer.writeVarInt(packet.action());
                buffer.writeVarInt(packet.value());
            }, buffer -> new MachineActionPayload(buffer.readVarLong(), buffer.readVarInt(), buffer.readVarInt()));
    *///?}

    public BlockPos blockPos() { return BlockPos.of(position); }
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
