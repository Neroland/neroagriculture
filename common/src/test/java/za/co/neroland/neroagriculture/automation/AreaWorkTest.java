package za.co.neroland.neroagriculture.automation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.core.BlockPos;

import org.junit.jupiter.api.Test;

class AreaWorkTest {
    @Test
    void radiusClampsToSevenBySevenThroughThirteenByThirteen() {
        assertEquals(3, AreaWork.radius(0), "no range upgrades = 7x7 (3 blocks each side)");
        assertEquals(4, AreaWork.radius(1));
        assertEquals(6, AreaWork.radius(3), "three range upgrades = 13x13");
        assertEquals(6, AreaWork.radius(99), "clamped at 13x13");
        assertEquals(3, AreaWork.radius(-5), "negative clamps to minimum");
    }

    @Test
    void columnCountsMatchTheSquare() {
        assertEquals(9, AreaWork.columns(1));
        assertEquals(49, AreaWork.columns(3));
        assertEquals(81, AreaWork.columns(4));
    }

    @Test
    void everyColumnIsVisitedExactlyOnceAndStaysInBounds() {
        BlockPos center = new BlockPos(10, 64, -7);
        int radius = 3;
        Set<Long> seen = new HashSet<>();
        for (int i = 0; i < AreaWork.columns(radius); i++) {
            BlockPos pos = AreaWork.columnAt(center, radius, i);
            assertEquals(center.getY(), pos.getY(), "work stays in the machine's plane");
            assertTrue(Math.abs(pos.getX() - center.getX()) <= radius, "within X radius");
            assertTrue(Math.abs(pos.getZ() - center.getZ()) <= radius, "within Z radius");
            assertTrue(seen.add(pos.asLong()), "no column visited twice");
        }
        assertEquals(AreaWork.columns(radius), seen.size());
        // Index equal to count wraps back to the first column.
        assertEquals(AreaWork.columnAt(center, radius, 0), AreaWork.columnAt(center, radius, AreaWork.columns(radius)));
    }

    @Test
    void cursorAdvancesModuloTheArea() {
        assertEquals(4, AreaWork.advanceCursor(0, 4, 1));
        assertEquals(0, AreaWork.advanceCursor(5, 4, 1), "9-column area wraps to 0");
    }
}
