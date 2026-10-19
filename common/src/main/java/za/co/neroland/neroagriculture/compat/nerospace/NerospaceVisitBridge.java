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
 *   <li><b>Not Core's link API:</b> Nerospace's {@code za.co.neroland.nerolandcore.link} module serves
 *       rockets, stations, planets, life support and the Star Guide — there is no visit section, and in
 *       any case that registry exists to answer companion clients about a player's own data, not to be
 *       a mod-to-mod query channel.</li>
 *   <li><b>Compile-time API, never a hard dependency:</b> Nerospace is a {@code compileOnly} dependency
 *       ({@code nerospace_version} in {@code gradle.properties}) and sibling interop must stay optional,
 *       so the only Nerospace types this class names live in the nested {@link History} holder, which
 *       the JVM links on first use and which is only reached once Nerospace is known to be loaded.</li>
 *   <li><b>Live visits — loader events plus vanilla dimension identity:</b> a Nerospace planet is,
 *       at data level, just the dimension id its materials are restricted to (e.g.
 *       {@code nerospace:cindara}). Player join and dimension-change events — wired by each loader
 *       entry point through {@link za.co.neroland.neroagriculture.compat.CompatContracts} — compare
 *       {@code player.level().dimension().identifier()} against the catalog's restrictions. Zero
 *       Nerospace code involved, so it works with every Nerospace version and costs one string
 *       comparison when Nerospace is absent.</li>
 *   <li><b>Historical visits — Nerospace's public {@code NerospaceVisits} API:</b> visits made before
 *       NeroAgriculture was installed are backfilled once per join from
 *       {@code za.co.neroland.nerospace.api.NerospaceVisits#visitedPlanets(MinecraftServer, UUID)}
 *       (Nerospace 1.0.3+), called directly through the {@link History} holder. This replaces an
 *       earlier reflective probe that guessed at method names; any failure degrades silently to live
 *       tracking only.</li>
 * </ul>
 *
 * <h2>Caching and cost</h2>
 * The cross-mod history query runs exactly once per player join; every later grant in the session is
 * event-driven off our own dimension-change hook, which doubles as the "new visit" invalidation — no
 * polling beyond the Fabric tick diff and no per-tick work. Milestone writes are idempotent
 * ({@code isObserved} is checked first) and Core persists them, so re-running a sync is harmless.
 *
 * <h2>Privacy (POPIA/GDPR)</h2>
 * This class persists nothing. It reads the joining player's own Nerospace visit list (planet ids only)
 * and uses it solely to grant that same player's milestones. The only per-player data it writes is the
 * milestone grant itself, which lives in Core's {@code MaterialMilestones} store and is therefore
 * covered by Core's {@code PlayerDataErasure} hook exactly like every other research milestone. Logs
 * carry material and dimension ids only — never player names or UUIDs.
 *
 * <p>The one in-memory per-player value, {@link #LAST_DIMENSION}, is session-scoped and dropped on
 * disconnect and on server stop, so it cannot outlive a session. It is nevertheless registered with
 * Core's erasure hook by {@link #register()}: an erasure request should leave no trace of a player
 * anywhere in this mod, including RAM, and "it would have been cleared eventually" is not an answer to
 * give a data subject.
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
     * operational data: never persisted, never logged, removed on disconnect and cleared on server stop
     * via {@link #reset()}. {@link #register()} additionally drops the row on a Core erasure request so
     * the guarantee is "gone now", not "gone when you next log out".
     */
    private static final Map<UUID, Identifier> LAST_DIMENSION = new ConcurrentHashMap<>();

    private static int tickCounter;

    private NerospaceVisitBridge() { }

    /**
     * Registers the session map with Core's shared erasure hook. Called once from common init, next to
     * the other {@code PlayerDataErasure} registrations.
     */
    public static void register() {
        za.co.neroland.nerolandcore.data.PlayerDataErasure.register(
                (server, uuid) -> LAST_DIMENSION.remove(uuid));
    }

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
     * (one direct API query; empty when Nerospace is absent).
     */
    public static void onPlayerJoin(ServerPlayer player) {
        if (!AgricultureConfig.NEROSPACE_VISITS.get()) return;
        grantForDimension(player, player.level().dimension().identifier());
        MinecraftServer server = player.level().getServer();
        if (server == null || !NerospaceEnvironmentBridge.nerospaceLoaded()) return;
        for (Identifier dimension : History.visitedDimensions(server, player.getUUID())) {
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

    /**
     * The only place in this class that names a Nerospace type. Linked by the JVM on first use, which
     * {@link #onPlayerJoin} only reaches once Nerospace is known to be loaded, so with Nerospace absent it
     * is never touched. Its signature carries no Nerospace types.
     */
    private static final class History {

        /** Set once the query has failed, so a broken or incompatible Nerospace is not retried every join. */
        private static volatile boolean disabled;

        private History() { }

        /** Dimension ids of every Nerospace planet {@code player} has visited; empty on any failure. */
        static java.util.List<Identifier> visitedDimensions(MinecraftServer server, UUID player) {
            if (disabled) return java.util.List.of();
            try {
                java.util.List<Identifier> out = new java.util.ArrayList<>();
                for (za.co.neroland.nerospace.api.PlanetId planet
                        : za.co.neroland.nerospace.api.NerospaceVisits.visitedPlanets(server, player)) {
                    out.add(planet.id());
                }
                return out;
            } catch (RuntimeException | LinkageError e) {
                disabled = true;
                NeroAgricultureCommon.LOGGER.info(
                        "[NeroAgriculture] Nerospace visit history unavailable ({}); planet visits are tracked "
                        + "live only.", e.getClass().getSimpleName());
                return java.util.List.of();
            }
        }
    }
}
