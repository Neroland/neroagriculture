package za.co.neroland.neroagriculture.catalog;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import za.co.neroland.neroagriculture.network.AgricultureNetwork;
import za.co.neroland.neroagriculture.network.MaterialCatalogSyncPayload;

public final class CatalogSync {
    private CatalogSync() { }
    public static void syncTo(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        if (server != null) AgricultureNetwork.sendToPlayer(player,
                MaterialCatalogSyncPayload.from(MaterialCatalog.forServer(server)));
    }
    public static void reloadAndSync(MinecraftServer server) {
        ResolvedCatalog catalog = MaterialCatalog.reload(server);
        MaterialCatalogSyncPayload payload = MaterialCatalogSyncPayload.from(catalog);
        server.getPlayerList().getPlayers().forEach(player -> AgricultureNetwork.sendToPlayer(player, payload));
    }
}
