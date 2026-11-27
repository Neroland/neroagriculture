package za.co.neroland.neroagriculture.fabric;

//? if >=1.20.5 {
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
//?} else {
/*import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.FriendlyByteBuf;

import za.co.neroland.neroagriculture.network.AgricultureNetwork;
import za.co.neroland.nerolandcore.network.legacy.LegacyPayload;

public final class FabricClientNetwork {
    private FabricClientNetwork() {
    }

    public static void register() {
        AgricultureNetwork.clientbound().forEach(FabricClientNetwork::registerReceiver);
    }

    private static <T extends LegacyPayload> void registerReceiver(AgricultureNetwork.Clientbound<T> packet) {
        ClientPlayNetworking.registerGlobalReceiver(packet.type().id(), (client, handler, buffer, response) -> {
            T payload = packet.codec().decode(buffer);
            client.execute(() -> packet.handler().accept(payload));
        });
    }

    static void send(LegacyPayload payload) {
        if (!ClientPlayNetworking.canSend(payload.type().id())) {
            return;
        }
        for (var packet : AgricultureNetwork.serverbound()) {
            if (packet.type().equals(payload.type())) {
                ClientPlayNetworking.send(packet.type().id(), encode(packet, payload));
                return;
            }
        }
        throw new IllegalArgumentException("Unregistered serverbound payload " + payload.type().id());
    }

    private static <T extends LegacyPayload> FriendlyByteBuf encode(AgricultureNetwork.Serverbound<T> packet,
            LegacyPayload payload) {
        FriendlyByteBuf buffer = PacketByteBufs.create();
        packet.codec().encode(buffer, packet.type().payloadClass().cast(payload));
        return buffer;
    }
}
*///?}
