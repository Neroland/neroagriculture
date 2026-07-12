package za.co.neroland.neroagriculture.balance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import za.co.neroland.neroagriculture.content.EssenceFamily;

/** Stage 6 net-conservation and per-tier cap invariants; pure logic, no running server. */
class TierBalanceTest {
    private static final int DEFAULT_CAP_BASE = 3;
    private static final int DEFAULT_CAP_STEP = 1;

    @Test
    void perTierCapIsMonotonicBoundedAndCoversDefaultMax() {
        int previous = Integer.MIN_VALUE;
        for (EssenceFamily tier : EssenceFamily.values()) {
            int cap = TierBalance.yieldCap(tier, DEFAULT_CAP_BASE, DEFAULT_CAP_STEP);
            assertTrue(cap >= 1, "cap must stay positive");
            assertTrue(cap > previous, "cap must grow with tier");
            assertTrue(cap >= TierBalance.defaultYieldMax(tier), "cap must not clip built-in defaults");
            previous = cap;
        }
    }

    @Test
    void capCollapsesToOneWhenOperatorZeroesBaseAndStep() {
        for (EssenceFamily tier : EssenceFamily.values()) {
            assertEquals(1, TierBalance.yieldCap(tier, 1, 0));
        }
    }

    @Test
    void noSingleCappedHarvestCanMintAResourceForAnyTier() {
        for (EssenceFamily tier : EssenceFamily.values()) {
            int cap = TierBalance.yieldCap(tier, DEFAULT_CAP_BASE, DEFAULT_CAP_STEP);
            assertTrue(TierBalance.singleHarvestCannotMintResource(tier, cap),
                    "one harvest must not convert back into a full resource for " + tier);
            // Even an operator who inflates the cap far beyond defaults stays net-conservative,
            // because defaultYieldMax still bounds the essence a single harvest actually produces.
            assertTrue(TierBalance.singleHarvestCannotMintResource(tier, 4096),
                    "inflated cap must still not mint a resource for " + tier);
        }
    }

    @Test
    void freshSeedsRequireInvestmentBeforeReachingMaximum() {
        for (EssenceFamily tier : EssenceFamily.values()) {
            assertEquals(1, TierBalance.defaultYieldMin(tier), "fresh seed must yield one unit");
            assertTrue(TierBalance.freshSeedBelowMax(tier), "fresh seed must sit below its maximum for " + tier);
            assertTrue(TierBalance.defaultRamp(tier) >= 32, "ramp must demand real harvest investment for " + tier);
        }
    }

    @Test
    void conversionCostAlwaysExceedsSeedSampleCost() {
        for (EssenceFamily tier : EssenceFamily.values()) {
            assertTrue(TierBalance.conversionCount(tier) >= TierBalance.samplesPerSeed(tier),
                    "recovering one resource must cost at least as much as one seed for " + tier);
        }
    }
}
