package za.co.neroland.neroagriculture.catalog;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import za.co.neroland.neroagriculture.food.FoodCatalog;
import za.co.neroland.neroagriculture.network.AgricultureNetwork;
import za.co.neroland.neroagriculture.network.MaterialCatalogSyncPayload;
import za.co.neroland.neroagriculture.network.SpeciesCatalogSyncPayload;

/** Pushes the bounded material and species display snapshots to clients; no player data is read or logged. */
public final class CatalogSync {
    private CatalogSync() { }
    public static void syncTo(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        if (server == null) return;
        AgricultureNetwork.sendToPlayer(player, MaterialCatalogSyncPayload.from(MaterialCatalog.forServer(server)));
        AgricultureNetwork.sendToPlayer(player, SpeciesCatalogSyncPayload.from(FoodCatalog.forServer(server)));
    }
    public static void reloadAndSync(MinecraftServer server) {
        ResolvedCatalog catalog = MaterialCatalog.reload(server);
        MaterialCatalogSyncPayload materials = MaterialCatalogSyncPayload.from(catalog);
        SpeciesCatalogSyncPayload species = SpeciesCatalogSyncPayload.from(FoodCatalog.reload(server));
        server.getPlayerList().getPlayers().forEach(player -> {
            AgricultureNetwork.sendToPlayer(player, materials);
            AgricultureNetwork.sendToPlayer(player, species);
        });
    }
}
