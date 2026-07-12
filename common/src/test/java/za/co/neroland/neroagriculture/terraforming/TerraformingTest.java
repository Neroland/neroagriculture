package za.co.neroland.neroagriculture.terraforming;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TerraformingTest {
    private static final int TOTAL = 6_000;

    @Test
    void stagesProgressDeterministicallyWithAccumulatedProgress() {
        assertEquals(TerraformingStage.DORMANT, TerraformingStage.of(false, 0, TOTAL), "unseeded is dormant");
        assertEquals(TerraformingStage.SEEDED, TerraformingStage.of(true, 0, TOTAL));
        assertEquals(TerraformingStage.SEEDED, TerraformingStage.of(true, 1_999, TOTAL));
        assertEquals(TerraformingStage.GROWING, TerraformingStage.of(true, 2_000, TOTAL));
        assertEquals(TerraformingStage.STABILISING, TerraformingStage.of(true, 4_000, TOTAL));
        assertEquals(TerraformingStage.STABILISING, TerraformingStage.of(true, 5_999, TOTAL));
        assertEquals(TerraformingStage.COMPLETE, TerraformingStage.of(true, TOTAL, TOTAL));
        assertTrue(TerraformingStage.of(true, TOTAL + 100, TOTAL).complete(), "over-progress stays complete");
    }

    @Test
    void regionContainmentIsABoundedSquare() {
        assertTrue(TerraformingRegions.contains(0, 0, 8, 8, 8), "the corner is inside the radius");
        assertTrue(TerraformingRegions.contains(0, 0, 8, -8, -8));
        assertFalse(TerraformingRegions.contains(0, 0, 8, 9, 0), "just past the radius is outside");
        assertFalse(TerraformingRegions.contains(0, 0, 8, 0, 9));
        assertTrue(TerraformingRegions.contains(100, -50, 4, 103, -47));
        assertFalse(TerraformingRegions.contains(100, -50, 4, 105, -50));
    }
}
