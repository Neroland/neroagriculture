package za.co.neroland.neroagriculture.crop;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;
import za.co.neroland.neroagriculture.catalog.ResolvedCatalog.Status;
import za.co.neroland.neroagriculture.content.FragmentTier;

class GrowBedInteractionTest {
    @Test void seedOnTopPlantsButSideClickOpensTheBed() {
        assertTrue(GrowBedInteraction.plantsFromFace(true, Direction.UP));
        assertFalse(GrowBedInteraction.plantsFromFace(true, Direction.NORTH));
        assertFalse(GrowBedInteraction.plantsFromFace(true, Direction.DOWN));
        assertFalse(GrowBedInteraction.plantsFromFace(false, Direction.UP));
    }

    @Test void waitingSeedReportsInvalidCatalogOrInsufficientTier() {
        assertNull(GrowBedInteraction.waitingReason(Status.ACTIVE, FragmentTier.TERRITE, FragmentTier.TERRITE, true, true));
        assertEquals(GrowthRules.BlockedReason.WRONG_BED,
                GrowBedInteraction.waitingReason(Status.ACTIVE, FragmentTier.FORGITE, FragmentTier.TERRITE, true, true));
        assertEquals(GrowthRules.BlockedReason.UNKNOWN_MATERIAL,
                GrowBedInteraction.waitingReason(Status.UNKNOWN, null, FragmentTier.TERRITE, true, true));
        assertEquals(GrowthRules.BlockedReason.DISABLED_MATERIAL,
                GrowBedInteraction.waitingReason(Status.DISABLED, FragmentTier.TERRITE, FragmentTier.TERRITE, true, true));
        assertEquals(GrowthRules.BlockedReason.GATE_CLOSED,
                GrowBedInteraction.waitingReason(Status.ACTIVE, FragmentTier.TERRITE, FragmentTier.TERRITE, false, true));
        assertEquals(GrowthRules.BlockedReason.WRONG_DIMENSION,
                GrowBedInteraction.waitingReason(Status.ACTIVE, FragmentTier.TERRITE, FragmentTier.TERRITE, true, false));
    }
}
