package za.co.neroland.neroagriculture.biofuel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BiofuelTest {
    private static final int COMPRESSION_RATIO = 9; // 9 fragment <-> 1 compacted block

    @Test
    void energyValueIsLinearAndNonNegative() {
        assertEquals(750L, Biofuel.energyNf(250, 3));
        assertEquals(0L, Biofuel.energyNf(-5, 3), "negative amount yields no energy");
        assertEquals(0L, Biofuel.energyNf(250, -1), "negative value yields no energy");
    }

    @Test
    void biofuelStaysBelowThePrimaryGenerationCeiling() {
        assertTrue(Biofuel.belowPrimaryCeiling(3, 8), "the default value must sit below the ceiling");
        assertFalse(Biofuel.belowPrimaryCeiling(8, 8), "reaching the ceiling is not below it");
        assertFalse(Biofuel.belowPrimaryCeiling(9, 8), "above the ceiling fails");
    }

    @Test
    void compactionRoundTripConservesExactly() {
        int blocks = 4;
        int fragment = blocks * COMPRESSION_RATIO;      // decompress
        assertEquals(36, fragment);
        assertEquals(blocks, fragment / COMPRESSION_RATIO, "compressing back conserves the exact count");
        assertEquals(0, fragment % COMPRESSION_RATIO, "no fragment is created or lost in the round trip");
    }
}
