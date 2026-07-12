package za.co.neroland.neroagriculture.machine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.resources.Identifier;

import org.junit.jupiter.api.Test;

import za.co.neroland.neroagriculture.content.EssenceCharge;
import za.co.neroland.neroagriculture.content.EssenceFamily;
import za.co.neroland.neroagriculture.content.MaterialVariant;

class FabricationRulesTest {
    @Test void allFourCondensationTransitionsAreAdjacentOnly() {
        EssenceFamily[] values = EssenceFamily.values();
        for (int index = 0; index < values.length - 1; index++) {
            assertTrue(FabricationRules.transitionAllowed(values[index], values[index + 1]));
        }
        assertFalse(FabricationRules.transitionAllowed(EssenceFamily.TERRAN, EssenceFamily.COLONIAL));
    }

    @Test void recipeCostsAndOverridesRemainDeterministic() {
        assertEquals(10, FabricationRules.energyPerTick(800, 80));
        assertEquals(11, FabricationRules.energyPerTick(801, 80));
        assertEquals(0, FabricationRules.energyPerTick(0, 20));
        assertThrows(IllegalArgumentException.class, () -> FabricationRules.energyPerTick(1, 0));
    }

    @Test void forgedOrMismatchedComponentsFailClosed() {
        Identifier iron = Identifier.parse("c:iron");
        MaterialVariant variant = MaterialVariant.of(iron, EssenceFamily.INDUSTRIAL);
        assertTrue(FabricationRules.materialMatches(variant, iron, EssenceFamily.INDUSTRIAL));
        assertFalse(FabricationRules.materialMatches(variant, iron, EssenceFamily.ORBITAL));
        assertTrue(FabricationRules.chargeMatches(EssenceCharge.of(EssenceFamily.INDUSTRIAL),
                EssenceFamily.INDUSTRIAL));
        assertFalse(FabricationRules.chargeMatches(EssenceCharge.of(EssenceFamily.ORBITAL),
                EssenceFamily.INDUSTRIAL));
    }

    @Test void outputPowerAndGateAreRecheckedAtCompletion() {
        assertTrue(FabricationRules.mayComplete(true, true, true, true, true));
        assertFalse(FabricationRules.mayComplete(true, false, true, true, true));
        assertFalse(FabricationRules.mayComplete(true, true, false, true, true));
        assertFalse(FabricationRules.mayComplete(true, true, true, false, true));
        assertFalse(FabricationRules.mayComplete(true, true, true, true, false));
    }
}
