package za.co.neroland.neroagriculture.lifecycle;

import za.co.neroland.neroagriculture.catalog.ClientMaterialCatalog;
import za.co.neroland.neroagriculture.catalog.ClientSpeciesCatalog;
import za.co.neroland.neroagriculture.network.ClientMachineMenuPositions;

/**
 * One entry point that clears every common client-scoped cache when the client disconnects, invoked
 * from each loader's client-disconnect hook (the client-side mirror of {@link ServerStateReset}).
 * Without this, server A's synced catalogs survive into a session on server B — or on a server
 * without this mod at all — and unpolled menu-position mailbox entries accumulate for the JVM's
 * lifetime. No player data is read or stored.
 */
public final class ClientStateReset {
    private ClientStateReset() { }

    /** Clear all client-session caches; loaders fire it from their client disconnect event. */
    public static void disconnected() {
        ClientMaterialCatalog.clear();
        ClientSpeciesCatalog.clear();
        ClientMachineMenuPositions.clear();
    }
}
