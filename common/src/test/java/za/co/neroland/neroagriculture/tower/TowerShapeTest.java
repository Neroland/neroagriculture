package za.co.neroland.neroagriculture.tower;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TowerShapeTest {
    @Test
    void formationHonoursTheHeightBounds() {
        assertFalse(TowerShape.valid(2, 3, 12), "below the minimum height is not a tower");
        assertTrue(TowerShape.valid(3, 3, 12), "exactly the minimum forms");
        assertTrue(TowerShape.valid(12, 3, 12), "exactly the maximum forms");
        assertFalse(TowerShape.valid(13, 3, 12), "above the maximum is invalid");
    }

    @Test
    void effectiveHeightClampsToTheMaximum() {
        assertEquals(5, TowerShape.effectiveHeight(5, 12));
        assertEquals(12, TowerShape.effectiveHeight(15, 12), "a taller stack caps out");
        assertEquals(0, TowerShape.effectiveHeight(0, 12));
    }

    @Test
    void slotCountScalesWithHeightAndIsBoundedByCapacity() {
        assertEquals(16, TowerShape.slots(4, 12, 4));
        assertEquals(48, TowerShape.slots(15, 12, 4), "slots never exceed the capacity");
        assertEquals(48, TowerShape.capacity(12, 4));
    }
}
