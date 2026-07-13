package za.co.neroland.neroagriculture.progression;

import net.minecraft.resources.Identifier;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.neroagriculture.NeroAgricultureCommon;
import za.co.neroland.neroagriculture.content.FragmentTier;

/**
 * NeroAgriculture's own progression-gate ids — a self-contained, standalone ladder that replaces the
 * former dependence on Core's arc gates ({@code industrial_power}/{@code reached_orbit}/{@code
 * first_colony}/{@code deep_space}). The mod is the sole opener of these gates (see the tier
 * milestones wired in the machine flow), so all five tiers are reachable with only Neroland Core
 * present.
 *
 * <p>The ids resolve to datapack gate definitions shipped under
 * {@code data/neroagriculture/neroland_gates/}; Core reads their scope + prerequisites. Tier 1
 * ({@link FragmentTier#TERRITE}) is open from the start and has no gate.
 */
public final class AgricultureGates {
    /** Tier 2 — unlocked once the player first upgrades a fragment / crafts a tier-1 resource seed. */
    public static final Identifier REFINEMENT = id("refinement");
    /** Tier 3 — unlocked after operating the infuser and accumulating fragment throughput. */
    public static final Identifier SYNTHESIS = id("synthesis");
    /** Tier 4 — unlocked after crafting a tier-3 resource seed. */
    public static final Identifier TRANSMUTATION = id("transmutation");
    /** Tier 5 — end-game milestone (a tier-4 seed plus a NeroFlux throughput threshold). */
    public static final Identifier ASCENSION = id("ascension");

    private AgricultureGates() { }

    /** The native gate that guards {@code tier}, or {@code null} for the always-open first tier. */
    @Nullable
    public static Identifier forTier(FragmentTier tier) {
        return switch (tier) {
            case TERRITE -> null;
            case FORGITE -> REFINEMENT;
            case ORBITE -> SYNTHESIS;
            case COLONITE -> TRANSMUTATION;
            case VOIDITE -> ASCENSION;
        };
    }

    /**
     * The gate a player earns by <em>producing</em> a fragment of {@code produced} — i.e. the gate that
     * guards the next tier up. Producing Territe (T1, ungated) opens {@link #REFINEMENT}; producing
     * Forgite opens {@link #SYNTHESIS}; and so on. Returns {@code null} at the top of the ladder. This is
     * what makes the ladder self-unlocking and standalone: each tier's output is the key to the next.
     */
    @Nullable
    public static Identifier gateUnlockedByProducing(FragmentTier produced) {
        FragmentTier[] tiers = FragmentTier.values();
        int next = produced.ordinal() + 1;
        return next < tiers.length ? forTier(tiers[next]) : null;
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(NeroAgricultureCommon.MOD_ID, path);
    }
}
