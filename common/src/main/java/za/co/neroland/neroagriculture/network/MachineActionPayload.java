package za.co.neroland.neroagriculture.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import za.co.neroland.neroagriculture.NeroAgricultureCommon;

/** Fixed-size client request. Semantic validation remains server-side. */
public record MachineActionPayload(long position, int action, int value) implements CustomPacketPayload {
    public static final Type<MachineActionPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(
            NeroAgricultureCommon.MOD_ID, "machine_action"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MachineActionPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_LONG, MachineActionPayload::position,
            ByteBufCodecs.VAR_INT, MachineActionPayload::action,
            ByteBufCodecs.VAR_INT, MachineActionPayload::value,
            MachineActionPayload::new);

    public BlockPos blockPos() { return BlockPos.of(position); }
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
