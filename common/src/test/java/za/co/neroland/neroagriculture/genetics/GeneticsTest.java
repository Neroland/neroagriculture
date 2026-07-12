package za.co.neroland.neroagriculture.genetics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class GeneticsTest {
    @Test
    void perTraitAndTotalCapsAreEnforcedOnConstruction() {
        Genetics forged = new Genetics(9, 9, 9, 9, 9);
        assertTrue(forged.yield() <= GeneticTrait.MAX_PER_TRAIT, "each trait clamped to 5");
        assertEquals(GeneticTrait.TOTAL_CAP, forged.total(), "total clamped to 15");
        // Reduction is deterministic from the highest-index trait down, so early traits keep their value.
        assertEquals(5, forged.yield());
        assertEquals(5, forged.speed());
        assertEquals(5, forged.hardiness());
        assertEquals(0, forged.oxygenOutput());
        assertEquals(0, forged.foodPotency());
    }

    @Test
    void negativeAndOversizedValuesAreClamped() {
        Genetics g = new Genetics(-3, 2, 99, 0, 1);
        assertEquals(0, g.yield());
        assertEquals(5, g.hardiness());
        assertTrue(g.total() <= GeneticTrait.TOTAL_CAP);
    }

    @Test
    void spliceTakesTheHigherParentTraitAndStaysCapped() {
        Genetics a = new Genetics(5, 0, 3, 0, 0);
        Genetics b = new Genetics(0, 5, 0, 5, 0);
        Genetics spliced = Genetics.splice(a, b);
        assertEquals(5, spliced.yield());
        assertEquals(5, spliced.speed());
        assertEquals(3, spliced.hardiness());
        assertTrue(spliced.total() <= GeneticTrait.TOTAL_CAP, "splice never exceeds the total cap");
    }

    @Test
    void mutationRaisesOneTraitDeterministicallyAndNeverPastCaps() {
        Genetics g = new Genetics(1, 1, 1, 0, 0);
        Genetics m1 = g.mutated(42);
        Genetics m2 = g.mutated(42);
        assertEquals(m1, m2, "mutation is deterministic for the same seed");
        assertEquals(g.total() + 1, m1.total(), "mutation raises the total by exactly one when there is room");
        Genetics full = new Genetics(5, 5, 5, 0, 0); // total 15
        assertEquals(full, full.mutated(7), "a maxed total cannot mutate further");
    }

    @Test
    void withReplacesASingleTraitUnderTheCaps() {
        Genetics g = Genetics.EMPTY.with(GeneticTrait.FOOD_POTENCY, 4);
        assertEquals(4, g.foodPotency());
        assertEquals(4, g.get(GeneticTrait.FOOD_POTENCY));
    }
}
