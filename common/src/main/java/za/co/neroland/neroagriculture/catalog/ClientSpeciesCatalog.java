package za.co.neroland.neroagriculture.catalog;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.resources.Identifier;

import za.co.neroland.neroagriculture.network.SpeciesCatalogSyncPayload;

/** Client-only species display cache with no server rules or player data. */
public final class ClientSpeciesCatalog {
    private static volatile Map<Identifier, SpeciesCatalogSyncPayload.Entry> entries = Map.of();
    private ClientSpeciesCatalog() { }
    public static void accept(SpeciesCatalogSyncPayload payload) {
        Map<Identifier, SpeciesCatalogSyncPayload.Entry> next = new LinkedHashMap<>();
        payload.entries().forEach(entry -> next.put(entry.id(), entry));
        entries = Collections.unmodifiableMap(next);
    }
    public static Map<Identifier, SpeciesCatalogSyncPayload.Entry> entries() { return entries; }
    /** Forget the previous server's catalog on disconnect (see {@code lifecycle.ClientStateReset}). */
    public static void clear() { entries = Map.of(); }
}
