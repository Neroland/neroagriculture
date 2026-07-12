package za.co.neroland.neroagriculture.greenhouse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.core.BlockPos;

import org.junit.jupiter.api.Test;

class GreenhouseValidationTest {
    private static final BlockPos CONTROLLER = new BlockPos(0, 0, 0);

    private static GreenhouseValidation.Passable of(Set<Long> passable) {
        return pos -> passable.contains(pos.asLong());
    }

    private static Set<Long> smallSealedPocket() {
        Set<Long> passable = new HashSet<>();
        passable.add(new BlockPos(1, 0, 0).asLong());
        passable.add(new BlockPos(2, 0, 0).asLong());
        passable.add(new BlockPos(1, 1, 0).asLong());
        passable.add(new BlockPos(1, 0, 1).asLong());
        return passable;
    }

    @Test
    void sealedPocketFormsWithinCap() {
        GreenhouseValidation.Result result = GreenhouseValidation.validate(CONTROLLER, of(smallSealedPocket()), 64);
        assertEquals(GreenhouseValidation.Structure.FORMED, result.structure());
        assertEquals(4, result.volume());
        assertEquals(null, result.leak());
    }

    @Test
    void exceedingTheVolumeCapReportsBreachWithLeak() {
        GreenhouseValidation.Result result = GreenhouseValidation.validate(CONTROLLER, of(smallSealedPocket()), 2);
        assertEquals(GreenhouseValidation.Structure.BREACHED, result.structure());
        assertNotNull(result.leak(), "a breach must report a leak frontier");
        assertTrue(result.volume() > 2);
    }

    @Test
    void aControllerWalledInSolidIsUnformed() {
        GreenhouseValidation.Result result = GreenhouseValidation.validate(CONTROLLER, of(Set.of()), 64);
        assertEquals(GreenhouseValidation.Structure.UNFORMED, result.structure());
        assertEquals(0, result.volume());
    }

    @Test
    void floodFillNeverEscapesThroughTheController() {
        // The controller position itself is passable in the predicate, but must be treated as a boundary.
        Set<Long> passable = smallSealedPocket();
        passable.add(CONTROLLER.asLong());
        GreenhouseValidation.Result result = GreenhouseValidation.validate(CONTROLLER, of(passable), 64);
        assertTrue(result.interior().stream().noneMatch(key -> key == CONTROLLER.asLong()),
                "controller must never be counted as interior");
    }
}
