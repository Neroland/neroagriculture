package za.co.neroland.neroagriculture.lifecycle;

import za.co.neroland.neroagriculture.catalog.MaterialCatalog;
import za.co.neroland.neroagriculture.cycle.Cycles;
import za.co.neroland.neroagriculture.greenhouse.GreenhouseIndex;
import za.co.neroland.neroagriculture.terraforming.TerraformingRegions;

/**
 * One entry point that clears every common static server-scoped cache when a server stops, invoked from
 * each loader's server-stopped hook. Without this the JVM-lifetime statics survive world unload — stale
 * terraformed regions and greenhouse interiors bleed into the next singleplayer world, cycle buckets go
 * stale, and {@link MaterialCatalog} pins the dead {@code MinecraftServer} graph in memory.
 */
public final class ServerStateReset {
    private ServerStateReset() { }

    /** Clear all server-scoped static state; safe to call on any thread, but loaders fire it on the server thread. */
    public static void serverStopped() {
        TerraformingRegions.clearAll();
        GreenhouseIndex.clearAll();
        Cycles.clearCache();
        MaterialCatalog.reset();
        za.co.neroland.neroagriculture.command.AgricultureGallery.clearRecords();
        za.co.neroland.neroagriculture.compat.nerospace.NerospaceVisitBridge.reset();
    }
}
