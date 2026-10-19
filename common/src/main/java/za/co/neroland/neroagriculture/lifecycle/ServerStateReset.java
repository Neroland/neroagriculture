package za.co.neroland.neroagriculture.lifecycle;

import net.minecraft.server.MinecraftServer;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.neroagriculture.catalog.MaterialCatalog;
import za.co.neroland.neroagriculture.cycle.Cycles;
import za.co.neroland.neroagriculture.greenhouse.GreenhouseIndex;
import za.co.neroland.neroagriculture.terraforming.TerraformingRegions;

/**
 * One entry point that clears every common static server-scoped cache when a server stops, invoked from
 * each loader's server-stopped hook. Without this the JVM-lifetime statics survive world unload — stale
 * terraformed regions and greenhouse interiors bleed into the next singleplayer world, cycle buckets go
 * stale, and {@link MaterialCatalog} pins the dead {@code MinecraftServer} graph in memory.
 *
 * <p>It also holds the running server, filled in by each loader's server-started hook. Core's link API
 * hands a provider a bare {@code UUID} and nothing else — no server, no player — so this is where the
 * link module finds the world it is answering about. Before the first world loads there is no server and
 * the link module answers empty, which is the honest result rather than a guess.
 */
public final class ServerStateReset {
    @Nullable private static volatile MinecraftServer server;

    private ServerStateReset() { }

    /** Record the running server; called from each loader's server-started hook. */
    public static void serverStarted(MinecraftServer started) {
        server = started;
    }

    /** The running server, or {@code null} before the first world load / after shutdown. */
    @Nullable
    public static MinecraftServer currentServer() {
        return server;
    }

    /** Clear all server-scoped static state; safe to call on any thread, but loaders fire it on the server thread. */
    public static void serverStopped() {
        server = null;
        TerraformingRegions.clearAll();
        GreenhouseIndex.clearAll();
        Cycles.clearCache();
        MaterialCatalog.reset();
        za.co.neroland.neroagriculture.command.AgricultureGallery.clearRecords();
        za.co.neroland.neroagriculture.compat.nerospace.NerospaceVisitBridge.reset();
        za.co.neroland.neroagriculture.tower.TowerOutputChannel.clearAll();
        za.co.neroland.neroagriculture.compat.nerospace.NerospaceEnvironmentBridge.reset();
        za.co.neroland.neroagriculture.link.AgricultureLinkEvents.reset();
    }
}
