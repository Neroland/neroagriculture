package za.co.neroland.neroagriculture.tower;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.resources.Identifier;

import org.junit.jupiter.api.Test;

import za.co.neroland.neroagriculture.content.FragmentTier;
import za.co.neroland.neroagriculture.genetics.Genetics;

class TowerSlotTest {
    @Test
    void aFreshSlotIsEmpty() {
        assertTrue(new TowerSlot().isEmpty());
    }

    @Test
    void plantingRecordsIdentityGeneticsAndHistoryAtAgeZero() {
        TowerSlot slot = new TowerSlot();
        Genetics genetics = new Genetics(3, 0, 0, 0, 0);
        slot.plant(Identifier.parse("c:iron"), FragmentTier.FORGITE, 5, genetics);
        assertFalse(slot.isEmpty());
        assertEquals(0, slot.age());
        assertEquals(5, slot.harvestCount());
        assertEquals(genetics, slot.genetics());
        assertFalse(slot.mature());
    }

    @Test
    void growthClampsToMaturityAndHarvestPreservesThePlant() {
        TowerSlot slot = new TowerSlot();
        slot.plant(Identifier.parse("c:coal"), FragmentTier.TERRITE, 0, Genetics.EMPTY);
        slot.grow(100);
        assertEquals(TowerSlot.MAX_AGE, slot.age(), "growth never exceeds maturity");
        assertTrue(slot.mature());
        slot.harvested();
        assertEquals(0, slot.age(), "harvest resets age");
        assertEquals(1, slot.harvestCount(), "harvest is recorded");
        assertFalse(slot.isEmpty(), "the plant stays in the slot after harvest");
    }

    @Test
    void clearEmptiesTheSlot() {
        TowerSlot slot = new TowerSlot();
        slot.plant(Identifier.parse("c:iron"), FragmentTier.FORGITE, 2, Genetics.EMPTY);
        slot.clear();
        assertTrue(slot.isEmpty());
        assertEquals(0, slot.harvestCount());
    }
}
