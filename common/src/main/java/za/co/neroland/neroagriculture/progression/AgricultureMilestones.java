package za.co.neroland.neroagriculture.progression;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

import za.co.neroland.neroagriculture.NeroAgricultureCommon;
import za.co.neroland.nerolandcore.progression.ProgressionGates;

/**
 * NeroAgriculture's personal <em>record</em> gates: player-scope Neroland Core gates that mark a
 * milestone the player reached once (first research, first harvest, running a greenhouse, running a
 * crop tower). They are opened by the mod at the moment of the milestone and read by quest packs
 * (NeroQuests' {@code gate_open} objective) and any other Core listener.
 *
 * <p>Unlike the tier gates in {@link AgricultureGates}, a record gate has no prerequisites and gates
 * nothing: it is never checked for access to a machine, recipe or tier, so nothing in play changes
 * when one is open. The definitions ship under {@code data/neroagriculture/neroland_gates/}.
 *
 * <p>Privacy: the only thing stored is the gate id in Core's per-player progression store, which
 * Core's privacy disclosure already covers (keyed by UUID, erased with Core's erasure hook). Nothing
 * is logged or broadcast.
 */
public final class AgricultureMilestones {
    /** The player completed a Seed Research Bench research. */
    public static final Identifier FIRST_RESEARCH = id("first_research");
    /** The player harvested a mature NeroAgriculture crop by hand. */
    public static final Identifier FIRST_HARVEST = id("first_harvest");
    /** The player opened a Greenhouse Controller while its greenhouse was formed and running. */
    public static final Identifier GREENHOUSE_KEEPER = id("greenhouse_keeper");
    /** The player opened a Crop Tower Controller while its tower was formed. */
    public static final Identifier TOWER_GROWER = id("tower_grower");

    private AgricultureMilestones() { }

    /**
     * Record {@code gate} for {@code player}. Opening is idempotent and cheap, so call sites may run
     * on every harvest or interaction; Core only fires a change the first time.
     */
    public static void record(ServerPlayer player, Identifier gate) {
        ProgressionGates.open(player, gate);
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(NeroAgricultureCommon.MOD_ID, path);
    }
}
