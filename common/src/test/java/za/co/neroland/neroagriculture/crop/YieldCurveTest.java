package za.co.neroland.neroagriculture.crop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import za.co.neroland.neroagriculture.catalog.MaterialDefinition;
import za.co.neroland.neroagriculture.content.EssenceFamily;

class YieldCurveTest {
    @Test
    void everyTierHonoursMinimumRampAndCapBoundaries() {
        for (EssenceFamily tier : EssenceFamily.values()) {
            int max = tier.ordinal() + 3;
            int ramp = 32 * (tier.ordinal() + 1);
            MaterialDefinition.Yield yield = new MaterialDefinition.Yield(1, max, ramp);
            assertEquals(1, YieldCurve.base(yield, -1));
            assertEquals(1, YieldCurve.base(yield, 0));
            assertEquals(max, YieldCurve.base(yield, ramp));
            assertEquals(max, YieldCurve.base(yield, ramp + 10_000));
            int midpoint = YieldCurve.base(yield, ramp / 2);
            assertEquals(1 + (max - 1) * (ramp / 2) / ramp, midpoint);
        }
    }

    @Test
    void globalMultiplierIsBoundedAndDeterministic() {
        MaterialDefinition.Yield yield = new MaterialDefinition.Yield(2, 6, 10);
        assertEquals(4, YieldCurve.scaled(yield, 5, 1.0));
        assertEquals(8, YieldCurve.scaled(yield, 5, 2.0));
        assertEquals(0, YieldCurve.scaled(yield, 5, -1.0));
    }

    @Test
    void freshSeedNeverStartsAtMaximum() {
        for (EssenceFamily tier : EssenceFamily.values()) {
            int max = tier.ordinal() + 3;
            int ramp = 32 * (tier.ordinal() + 1);
            MaterialDefinition.Yield yield = new MaterialDefinition.Yield(1, max, ramp);
            int cap = max + 5; // generous cap so the ramp, not the cap, governs a fresh seed
            assertEquals(1, YieldCurve.scaledCapped(yield, 0, 1.0, cap), "fresh seed must yield the minimum");
            assertTrue(YieldCurve.scaledCapped(yield, 0, 1.0, cap)
                    < YieldCurve.maxCapped(yield, 1.0, cap), "fresh seed must be below max for " + tier);
        }
    }

    @Test
    void tierCapClampsEvenAnInflatedDefinitionAndMultiplier() {
        MaterialDefinition.Yield yield = new MaterialDefinition.Yield(1, 4096, 10);
        assertEquals(3, YieldCurve.scaledCapped(yield, 10, 100.0, 3), "cap must clamp overrides");
        assertEquals(3, YieldCurve.nextCapped(yield, 10, 100.0, 3));
        assertEquals(3, YieldCurve.maxCapped(yield, 100.0, 3));
    }

    @Test
    void nextAndMaxAreMonotonicAcrossTheRamp() {
        MaterialDefinition.Yield yield = new MaterialDefinition.Yield(1, 6, 10);
        int cap = 100;
        for (int harvests = 0; harvests < 12; harvests++) {
            assertTrue(YieldCurve.nextCapped(yield, harvests, 1.0, cap)
                    >= YieldCurve.scaledCapped(yield, harvests, 1.0, cap), "next must never regress");
            assertTrue(YieldCurve.scaledCapped(yield, harvests, 1.0, cap)
                    <= YieldCurve.maxCapped(yield, 1.0, cap), "current must never exceed max");
        }
    }
}
