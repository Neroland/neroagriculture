package za.co.neroland.neroagriculture.environment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class OxygenContributionTest {
    @Test
    void oxygenScalesWithMaturity() {
        assertEquals(0, OxygenContribution.perCrop(6, 0, 7, 0), "an immature plant contributes nothing");
        assertEquals(2, OxygenContribution.perCrop(6, 3, 7, 0), "partial maturity scales linearly");
        assertEquals(6, OxygenContribution.perCrop(6, 7, 7, 0), "a mature plant contributes its full base");
    }

    @Test
    void oxygenGeneticsAddOnTop() {
        assertEquals(8, OxygenContribution.perCrop(6, 7, 7, 2), "genetics add to base production");
        assertEquals(3, OxygenContribution.perCrop(0, 7, 7, 3), "a non-flora crop still contributes its genetics");
    }

    @Test
    void theHardPerVolumeCapBoundsContributionRegardlessOfPlantCount() {
        // 64-block interior, cap 8 per 32 volume -> 2 units -> cap 16.
        assertEquals(16, OxygenContribution.capped(1000, 64, 8, 32), "a swarm of plants cannot exceed the cap");
        assertEquals(5, OxygenContribution.capped(5, 64, 8, 32), "below the cap the real value passes through");
        assertEquals(8, OxygenContribution.capped(1000, 0, 8, 32), "a minimum of one volume unit always applies");
    }

    @Test
    void contributionIsNeverNegative() {
        assertTrue(OxygenContribution.perCrop(-5, -3, 7, -9) >= 0);
        assertTrue(OxygenContribution.capped(-100, 64, 8, 32) >= 0);
    }
}
