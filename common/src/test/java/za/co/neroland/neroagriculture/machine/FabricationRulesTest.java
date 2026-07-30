package za.co.neroland.neroagriculture.machine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.resources.Identifier;

import org.junit.jupiter.api.Test;

import za.co.neroland.neroagriculture.content.FragmentCharge;
import za.co.neroland.neroagriculture.content.FragmentTier;
import za.co.neroland.neroagriculture.content.MaterialVariant;

class FabricationRulesTest {
    @Test void allFourCondensationTransitionsAreAdjacentOnly() {
        FragmentTier[] values = FragmentTier.values();
        for (int index = 0; index < values.length - 1; index++) {
            assertTrue(FabricationRules.transitionAllowed(values[index], values[index + 1]));
        }
        assertFalse(FabricationRules.transitionAllowed(FragmentTier.TERRITE, FragmentTier.COLONITE));
    }

    @Test void recipeCostsAndOverridesRemainDeterministic() {
        assertEquals(10, FabricationRules.energyPerTick(800, 80));
        assertEquals(11, FabricationRules.energyPerTick(801, 80));
        assertEquals(0, FabricationRules.energyPerTick(0, 20));
        assertThrows(IllegalArgumentException.class, () -> FabricationRules.energyPerTick(1, 0));
    }

    @Test void forgedOrMismatchedComponentsFailClosed() {
        Identifier iron = Identifier.parse("c:iron");
        MaterialVariant variant = MaterialVariant.of(iron, FragmentTier.FORGITE);
        assertTrue(FabricationRules.materialMatches(variant, iron, FragmentTier.FORGITE));
        assertFalse(FabricationRules.materialMatches(variant, iron, FragmentTier.ORBITE));
        assertTrue(FabricationRules.chargeMatches(FragmentCharge.of(FragmentTier.FORGITE),
                FragmentTier.FORGITE));
        assertFalse(FabricationRules.chargeMatches(FragmentCharge.of(FragmentTier.ORBITE),
                FragmentTier.FORGITE));
    }

    @Test void outputPowerAndGateAreRecheckedAtCompletion() {
        assertTrue(FabricationRules.mayComplete(true, true, true, true, true));
        assertFalse(FabricationRules.mayComplete(true, false, true, true, true));
        assertFalse(FabricationRules.mayComplete(true, true, false, true, true));
        assertFalse(FabricationRules.mayComplete(true, true, true, false, true));
        assertFalse(FabricationRules.mayComplete(true, true, true, true, false));
    }

    @Test void fragmentsPerSeedScalesWithTierAndStaysPositive() {
        assertEquals(2, FabricationRules.fragmentsPerSeed(FragmentTier.TERRITE));
        assertEquals(3, FabricationRules.fragmentsPerSeed(FragmentTier.FORGITE));
        assertEquals(6, FabricationRules.fragmentsPerSeed(FragmentTier.VOIDITE));
        for (FragmentTier tier : FragmentTier.values()) {
            assertTrue(FabricationRules.fragmentsPerSeed(tier) >= 1);
        }
    }
}
