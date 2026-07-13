package za.co.neroland.neroagriculture.progression;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import net.minecraft.resources.Identifier;

import org.junit.jupiter.api.Test;

import za.co.neroland.neroagriculture.content.FragmentTier;

/** The native tier ladder must be self-unlocking standalone: producing tier N opens tier N+1's gate. */
class AgricultureGatesTest {

    @Test
    void tierOneIsOpenAndHigherTiersMapToNativeGates() {
        assertNull(AgricultureGates.forTier(FragmentTier.TERRITE));
        assertEquals(AgricultureGates.REFINEMENT, AgricultureGates.forTier(FragmentTier.FORGITE));
        assertEquals(AgricultureGates.SYNTHESIS, AgricultureGates.forTier(FragmentTier.ORBITE));
        assertEquals(AgricultureGates.TRANSMUTATION, AgricultureGates.forTier(FragmentTier.COLONITE));
        assertEquals(AgricultureGates.ASCENSION, AgricultureGates.forTier(FragmentTier.VOIDITE));
    }

    @Test
    void gatesAreNamespacedToNeroAgriculture() {
        assertEquals(Identifier.parse("neroagriculture:refinement"), AgricultureGates.REFINEMENT);
        assertEquals(Identifier.parse("neroagriculture:synthesis"), AgricultureGates.SYNTHESIS);
        assertEquals(Identifier.parse("neroagriculture:transmutation"), AgricultureGates.TRANSMUTATION);
        assertEquals(Identifier.parse("neroagriculture:ascension"), AgricultureGates.ASCENSION);
    }

    @Test
    void producingEachTierUnlocksTheNextTiersGate() {
        assertEquals(AgricultureGates.REFINEMENT, AgricultureGates.gateUnlockedByProducing(FragmentTier.TERRITE));
        assertEquals(AgricultureGates.SYNTHESIS, AgricultureGates.gateUnlockedByProducing(FragmentTier.FORGITE));
        assertEquals(AgricultureGates.TRANSMUTATION, AgricultureGates.gateUnlockedByProducing(FragmentTier.ORBITE));
        assertEquals(AgricultureGates.ASCENSION, AgricultureGates.gateUnlockedByProducing(FragmentTier.COLONITE));
        assertNull(AgricultureGates.gateUnlockedByProducing(FragmentTier.VOIDITE));
    }
}
