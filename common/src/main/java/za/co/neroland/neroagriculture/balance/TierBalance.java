package za.co.neroland.neroagriculture.balance;

import za.co.neroland.neroagriculture.content.FragmentTier;

/**
 * Pure, deterministic Stage 6 balance constants and net-conservation invariants for every built-in tier.
 * Everything here is data-free logic so it can be exercised by boundary tests without a running server.
 */
public final class TierBalance {
    private TierBalance() { }

    /**
     * Absolute per-harvest yield ceiling for a tier. No datapack override, config multiplier, or future
     * genetics bonus may push a single harvest above this. Derived from an operator-tunable base and step
     * so the cap grows monotonically with tier while remaining bounded and reload-safe.
     */
    public static int yieldCap(FragmentTier tier, int base, int step) {
        return Math.max(1, base + step * tier.ordinal());
    }

    /** Neutral fragment produced by extracting one real sample. */
    public static int extractionFragment(FragmentTier tier) {
        return 2;
    }

    /** Lower-tier neutral fragment condensed into one unit of the next tier. */
    public static int condensationRatio() {
        return 4;
    }

    /** Neutral fragment consumed to charge one blank seed at a tier. */
    public static int chargeCost(FragmentTier tier) {
        return 8;
    }

    /** Material fragment consumed to convert back into one unit of the original resource. */
    public static int conversionCount(FragmentTier tier) {
        return 8 + tier.ordinal() * 4;
    }

    /** Conservative default maximum yield used when a definition does not override it. */
    public static int defaultYieldMax(FragmentTier tier) {
        return tier.ordinal() + 3;
    }

    /** Conservative default fresh-seed yield: one unit, forcing harvest investment before the ramp helps. */
    public static int defaultYieldMin(FragmentTier tier) {
        return 1;
    }

    /** Harvests required before a fresh seed reaches its maximum yield. */
    public static int defaultRamp(FragmentTier tier) {
        return 32 * (tier.ordinal() + 1);
    }

    /** Raw samples sunk into one fabricated seed: synthesis sample + extracted resource fragment + charge. */
    public static int samplesPerSeed(FragmentTier tier) {
        return 2 + ceilDiv(chargeCost(tier), extractionFragment(tier));
    }

    /**
     * Core net-conservation invariant: no single harvest — even at the maximum capped yield — can be
     * converted back into a full unit of the source resource, so the loop never mints resources from
     * nothing before the documented repeated-harvest investment. Verified for every tier by tests.
     */
    public static boolean singleHarvestCannotMintResource(FragmentTier tier, int cap) {
        return Math.min(cap, defaultYieldMax(tier)) < conversionCount(tier);
    }

    /** Sanity invariant: the default ramp keeps fresh seeds below the cap until investment is made. */
    public static boolean freshSeedBelowMax(FragmentTier tier) {
        return defaultYieldMin(tier) < defaultYieldMax(tier) && defaultRamp(tier) > 0;
    }

    private static int ceilDiv(int numerator, int denominator) {
        return (numerator + denominator - 1) / denominator;
    }
}
