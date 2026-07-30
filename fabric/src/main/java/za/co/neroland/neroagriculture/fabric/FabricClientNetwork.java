package za.co.neroland.neroagriculture.fabric;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import za.co.neroland.neroagriculture.network.AgricultureNetwork;

/**
 * The client-only half of Fabric networking. {@link ClientPlayNetworking} does not exist on a
 * dedicated server, so every reference to it lives in this class alone; {@link FabricNetwork} (loaded
 * on both sides via ServiceLoader) only <em>names</em> this class from code paths that can never run
 * server-side, which keeps the server from ever classloading it — the canonical Fabric split.
 */
public final class FabricClientNetwork {
    private FabricClientNetwork() { }

    /** Attach the clientbound receivers; invoked from the client entry point only. */
    public static void register() {
        for (AgricultureNetwork.Clientbound<?> payload : AgricultureNetwork.clientbound()) registerReceiver(payload);
    }

    private static <T extends CustomPacketPayload> void registerReceiver(AgricultureNetwork.Clientbound<T> payload) {
        ClientPlayNetworking.registerGlobalReceiver(payload.type(), (value, context) ->
                context.client().execute(() -> payload.handler().accept(value)));
    }

    /** Client → server send; only ever reached from client code (screens). */
    static void send(CustomPacketPayload payload) {
        ClientPlayNetworking.send(payload);
    }
}
