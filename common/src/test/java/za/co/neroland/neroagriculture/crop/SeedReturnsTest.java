package za.co.neroland.neroagriculture.crop;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SeedReturnsTest {
    @Test void firstHarvestUsesTheConfiguredChance() {
        assertTrue(SeedReturns.awards(0, 10, 9));
        assertFalse(SeedReturns.awards(0, 10, 10));
        assertFalse(SeedReturns.awards(0, 0, 0));
    }

    @Test void subsequentHarvestsCannotDuplicateAgainEvenAtMaximumChance() {
        assertFalse(SeedReturns.awards(1, 100, 0));
        assertFalse(SeedReturns.awards(100, 100, 0));
        assertFalse(SeedReturns.awards(0, 100, -1));
    }
}
