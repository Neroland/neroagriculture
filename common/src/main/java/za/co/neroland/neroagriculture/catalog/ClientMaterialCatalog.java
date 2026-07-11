package za.co.neroland.neroagriculture.catalog;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.resources.Identifier;

import za.co.neroland.neroagriculture.network.MaterialCatalogSyncPayload;

/** Client-only display cache with no server rules or player data. */
public final class ClientMaterialCatalog {
    private static volatile Map<Identifier, MaterialCatalogSyncPayload.Entry> entries = Map.of();
    private ClientMaterialCatalog() { }
    public static void accept(MaterialCatalogSyncPayload payload) {
        Map<Identifier, MaterialCatalogSyncPayload.Entry> next = new LinkedHashMap<>();
        payload.entries().forEach(entry -> next.put(entry.id(), entry));
        entries = Collections.unmodifiableMap(next);
    }
    public static Map<Identifier, MaterialCatalogSyncPayload.Entry> entries() { return entries; }
}
