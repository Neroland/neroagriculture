package za.co.neroland.neroagriculture.crop;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
