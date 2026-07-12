package za.co.neroland.neroagriculture.genetics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PollinationTest {
    @Test
    void rollHonoursTheChanceBounds() {
        assertFalse(Pollination.roll(12345L, 0), "0% never succeeds");
        assertTrue(Pollination.roll(12345L, 100), "100% always succeeds");
    }

    @Test
    void rollIsDeterministicForTheSameSeed() {
        assertEquals(Pollination.roll(999L, 50), Pollination.roll(999L, 50));
    }

    @Test
    void rollIsApproximatelyUniformOverManySeeds() {
        int hits = 0;
        for (long seed = 0; seed < 4000; seed++) {
            if (Pollination.roll(seed, 25)) hits++;
        }
        double rate = hits / 4000.0;
        assertTrue(rate > 0.18 && rate < 0.32, "≈25% expected, got " + rate);
    }

    @Test
    void childGeneticsSpliceParentsAndStayCapped() {
        Genetics a = new Genetics(5, 0, 0, 0, 0);
        Genetics b = new Genetics(0, 5, 5, 0, 0);
        Genetics child = Pollination.childGenetics(a, b, false, 1L);
        assertEquals(Genetics.splice(a, b), child);
        Genetics mutated = Pollination.childGenetics(a, b, true, 1L);
        assertTrue(mutated.total() <= GeneticTrait.TOTAL_CAP, "mutated child never exceeds the total cap");
    }
}
