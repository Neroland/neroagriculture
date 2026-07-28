package za.co.neroland.neroagriculture.compat.nerospace;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import za.co.neroland.neroagriculture.NeroAgricultureCommon;
import za.co.neroland.neroagriculture.catalog.MaterialCatalog;
import za.co.neroland.neroagriculture.catalog.ResolvedMaterial;
import za.co.neroland.neroagriculture.config.AgricultureConfig;
import za.co.neroland.nerolandcore.progression.MaterialMilestoneDefinitions;
import za.co.neroland.nerolandcore.progression.MaterialMilestones;
import za.co.neroland.nerolandcore.progression.MaterialObservation;

/**
 * Runtime-guarded Nerospace planet-visit adapter. Planet-bound catalog materials (a
 * {@code worldRestriction} in the {@code nerospace} namespace) can only earn Core's
 * {@code material_discovered} milestone through a planet visit — the Seed Research Bench deliberately
 * refuses owner-mod/pickup evidence for them. This bridge is what records that visit evidence, so with
 * Nerospace installed those materials become researchable; without it they simply stay locked (their
 * dimensions cannot exist, so nothing here ever fires).
 *
 * <h2>Mechanism (decision record)</h2>
 * <ul>
 *   <li><b>Not Core's link API:</b> Nerospace registers no {@code za.co.neroland.nerolandcore.link}
 *       module (verified against the Nerospace source), so there is nothing in the link registry to
 *       query for visits.</li>
 *   <li><b>Not a compile-time import:</b> Nerospace is not on NeroAgriculture's compile classpath (no
 *       {@code nerospace} pin in {@code gradle.properties}) and sibling interop must stay optional, so
 *       {@code common/} cannot reference {@code za.co.neroland.nerospace} types.</li>
 *   <li><b>Live visits — loader events plus vanilla dimension identity:</b> a Nerospace planet is,
 *       at data level, just the dimension id its materials are restricted to (e.g.
 *       {@code nerospace:cindara}). Player join and dimension-change events — wired by each loader
 *       entry point through {@link za.co.neroland.neroagriculture.compat.CompatContracts} — compare
 *       {@code player.level().dimension().identifier()} against the catalog's restrictions. Zero
 *       Nerospace code involved, so it works with every Nerospace version and costs one string
 *       comparison when Nerospace is absent.</li>
 *   <li><b>Historical visits — reflection over the documented {@code nerospace.api} facade:</b>
 *       visits made before NeroAgriculture was added are backfilled once per join via
 *       {@link NerospaceVisitHistory}, which reflectively queries the semver-stable
 *       {@code za.co.neroland.nerospace.api.NerospaceVisits} surface negotiated for the
 *       Nerospace {@code >= 1.0.0-beta.8} API floor (BETA-0.1.0-PLAN Stage 1). Reflection is probed
 *       once, version-checked against the floor, and fails silently to "no history" on any absence or
 *       mismatch — live tracking keeps working regardless.</li>
 * </ul>
 *
 * <h2>Caching and cost</h2>
 * The cross-mod (reflective) query runs exactly once per player join; every later grant in the session
 * is event-driven off our own dimension-change hook, which doubles as the "new visit" invalidation —
 * no polling, no per-tick work, no repeated cross-mod calls. Milestone writes are idempotent
 * ({@code isObserved} is checked first) and Core persists them, so re-running a sync is harmless.
 *
 * <h2>Privacy (POPIA/GDPR)</h2>
 * This class stores nothing. The only per-player data it touches is the milestone grant itself, which
 * lives in Core's {@code MaterialMilestones} store and is therefore covered by Core's
 * {@code PlayerDataErasure} hook exactly like every other research milestone. Logs carry material and
 * dimension ids only — never player names or UUIDs.
 *
 * <p>Disable with {@code compat.nerospace_visits = false} (default {@code true}).</p>
 */
public final class NerospaceVisitBridge {

    /** The dimension namespace whose restrictions the research bench treats as visit-gated. */
    private static final String NEROSPACE_NAMESPACE = "nerospace";

    /** Poll cadence for {@link #onServerTick} on loaders without a dimension-change event (Fabric). */
    private static final int POLL_INTERVAL_TICKS = 100;

    /**
     * Session-only last-seen dimension per player, fed exclusively by the tick poll. Transient
     * operational data (never persisted, never logged), removed on disconnect and cleared on server
     * stop via {@link #reset()} — so it needs no erasure-hook coverage.
     */
    private static final Map<UUID, Identifier> LAST_DIMENSION = new ConcurrentHashMap<>();

    private static int tickCounter;

    private NerospaceVisitBridge() { }

    /**
     * Tick-driven dimension diff for loaders whose API ships no server-side player dimension-change
     * event (Fabric): every {@value #POLL_INTERVAL_TICKS} ticks, compare each online player's dimension
     * with the last seen one and treat a change as a live visit. First observation only seeds the map —
     * the join hook already granted the login dimension.
     */
    public static void onServerTick(MinecraftServer server) {
        if (!AgricultureConfig.NEROSPACE_VISITS.get()) return;
        if (++tickCounter < POLL_INTERVAL_TICKS) return;
        tickCounter = 0;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            Identifier current = player.level().dimension().identifier();
            Identifier previous = LAST_DIMENSION.put(player.getUUID(), current);
            if (previous != null && !previous.equals(current)) grantForDimension(player, current);
        }
    }

    /** Drop the disconnecting player's transient last-seen entry (data minimisation). */
    public static void playerDisconnected(ServerPlayer player) {
        LAST_DIMENSION.remove(player.getUUID());
    }

    /** Clear all transient session state; invoked from the common server-stopped reset. */
    public static void reset() {
        LAST_DIMENSION.clear();
        tickCounter = 0;
    }

    /**
     * Player joined: record the dimension they logged into, then backfill historical Nerospace visits
     * (one reflective query; empty when Nerospace is absent or below the {@code 1.0.0-beta.8} floor).
     */
    public static void onPlayerJoin(ServerPlayer player) {
        if (!AgricultureConfig.NEROSPACE_VISITS.get()) return;
        grantForDimension(player, player.level().dimension().identifier());
        for (Identifier dimension : NerospaceVisitHistory.visitedDimensions(
                player.level().getServer(), player.getUUID())) {
            grantForDimension(player, dimension);
        }
    }

    /** Player changed dimension: a live visit — the event-driven counterpart to the join backfill. */
    public static void onDimensionChange(ServerPlayer player) {
        if (!AgricultureConfig.NEROSPACE_VISITS.get()) return;
        grantForDimension(player, player.level().dimension().identifier());
    }

    /**
     * Grant {@code material_discovered} for every catalog material bound to {@code dimension}. Only
     * {@code nerospace}-namespace dimensions qualify — other restrictions stay on the normal
     * owner-mod/pickup evidence path, matching the research bench's visit-gate special case.
     */
    private static void grantForDimension(ServerPlayer player, Identifier dimension) {
        if (dimension == null || !NEROSPACE_NAMESPACE.equals(dimension.getNamespace())) return;
        MinecraftServer server = player.level().getServer();
        if (server == null) return;
        int granted = 0;
        for (ResolvedMaterial material : MaterialCatalog.forServer(server).all().values()) {
            var definition = material.definition();
            if (definition.worldRestriction() == null
                    || !definition.worldRestriction().dimension().equals(dimension)) continue;
            if (MaterialMilestones.isObserved(player, MaterialMilestoneDefinitions.MATERIAL_DISCOVERED,
                    definition.id())) continue;
            // A planet visit is Nerospace-authoritative evidence for its own planet-bound materials.
            MaterialMilestones.observe(player, MaterialMilestoneDefinitions.MATERIAL_DISCOVERED,
                    definition.id(), MaterialObservation.OWNER_MOD);
            granted++;
        }
        if (granted > 0) {
            NeroAgricultureCommon.LOGGER.debug(
                    "[NeroAgriculture] Planet visit to {} discovered {} planet-bound material(s).",
                    dimension, granted);
        }
    }
}
