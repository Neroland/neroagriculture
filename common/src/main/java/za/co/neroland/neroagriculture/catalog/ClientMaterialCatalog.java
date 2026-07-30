package za.co.neroland.neroagriculture.catalog;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import net.minecraft.resources.Identifier;

import za.co.neroland.neroagriculture.network.MaterialCatalogSyncPayload;

/** Client-only display cache with no server rules or player data. */
public final class ClientMaterialCatalog {
    private static volatile Map<Identifier, MaterialCatalogSyncPayload.Entry> entries = Map.of();
    /** Bumped on every accepted sync/clear so screens can cache derived views (e.g. panel rows) cheaply. */
    private static final AtomicInteger GENERATION = new AtomicInteger();
    private ClientMaterialCatalog() { }
    public static void accept(MaterialCatalogSyncPayload payload) {
        Map<Identifier, MaterialCatalogSyncPayload.Entry> next = new LinkedHashMap<>();
        payload.entries().forEach(entry -> next.put(entry.id(), entry));
        entries = Collections.unmodifiableMap(next);
        GENERATION.incrementAndGet();
    }
    public static Map<Identifier, MaterialCatalogSyncPayload.Entry> entries() { return entries; }
    /** Monotonic change counter; a cached view built at generation N is stale once this differs. */
    public static int generation() { return GENERATION.get(); }
    /** Forget the previous server's catalog on disconnect (see {@code lifecycle.ClientStateReset}). */
    public static void clear() {
        entries = Map.of();
        GENERATION.incrementAndGet();
    }
}
