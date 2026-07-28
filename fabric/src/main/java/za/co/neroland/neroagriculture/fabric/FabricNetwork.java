package za.co.neroland.neroagriculture.fabric;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

import za.co.neroland.neroagriculture.network.AgricultureNetwork;
import za.co.neroland.neroagriculture.platform.NetworkPlatform;

/**
 * Common-side Fabric networking. This class is ServiceLoader-instantiated on the dedicated server, so
 * it must never reference client-only Fabric API ({@code ClientPlayNetworking}) — those live in
 * {@link FabricClientNetwork}, which only client-side code paths ever classload.
 */
public final class FabricNetwork implements NetworkPlatform {
    public static void register() {
        for (AgricultureNetwork.Clientbound<?> payload : AgricultureNetwork.clientbound()) registerType(payload);
        for (AgricultureNetwork.Serverbound<?> payload : AgricultureNetwork.serverbound()) register(payload);
    }

    /** Kept as the client entry point's hook; delegates so the client-only class loads lazily there. */
    public static void registerClient() {
        FabricClientNetwork.register();
    }

    private static <T extends CustomPacketPayload> void registerType(AgricultureNetwork.Clientbound<T> payload) {
        PayloadTypeRegistry.clientboundPlay().register(payload.type(), payload.codec());
    }

    private static <T extends CustomPacketPayload> void register(AgricultureNetwork.Serverbound<T> payload) {
        PayloadTypeRegistry.serverboundPlay().register(payload.type(), payload.codec());
        ServerPlayNetworking.registerGlobalReceiver(payload.type(), (value, context) -> {
            ServerPlayer player = context.player();
            player.level().getServer().execute(() -> payload.handler().accept(value, player));
        });
    }

    @Override public void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) { ServerPlayNetworking.send(player, payload); }
    @Override public void sendToServer(CustomPacketPayload payload) { FabricClientNetwork.send(payload); }
}
