package za.co.neroland.neroagriculture.crop;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class GrowthPacingTest {
    @Test
    void starterCropGrowsFasterAndKeepsSpeedImprovements() {
        assertEquals(1.0, GrowthPacing.chance(1.0, 1.0));
        assertEquals(2, GrowthPacing.ageStep(1));
        assertEquals(3, GrowthPacing.ageStep(2));
    }

    @Test
    void modifiersAndLaterTiersRemainBounded() {
        assertEquals(0.0, GrowthPacing.chance(1.0, 0.0));
        assertEquals(0.5, GrowthPacing.chance(0.5, 1.0));
        assertEquals(1.0, GrowthPacing.chance(100.0, 1.0));
        assertEquals(2, GrowthPacing.ageStep(0));
    }
}
