package za.co.neroland.neroagriculture.progression;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import za.co.neroland.neroagriculture.content.FragmentTier;

/** The sibling overlay must never gate standalone play, and must honour the auto/on/off config. */
class SiblingOverlaysTest {

    @Test
    void autoModeNeverRequiresAnArcGateWhenTheOpenerModIsAbsent() {
        for (FragmentTier tier : FragmentTier.values()) {
            assertFalse(SiblingOverlays.requiresArcGate(tier, "auto", false),
                    () -> "standalone (opener absent) must never require an arc gate at " + tier);
        }
    }

    @Test
    void autoModeRequiresTheArcGateForGatedTiersWhenTheOpenerIsLoaded() {
        assertFalse(SiblingOverlays.requiresArcGate(FragmentTier.TERRITE, "auto", true), "tier 1 has no arc gate");
        assertTrue(SiblingOverlays.requiresArcGate(FragmentTier.FORGITE, "auto", true));
        assertTrue(SiblingOverlays.requiresArcGate(FragmentTier.VOIDITE, "auto", true));
    }

    @Test
    void onModeAlwaysRequiresGatedTiersAndOffNeverDoes() {
        assertTrue(SiblingOverlays.requiresArcGate(FragmentTier.ORBITE, "on", false));
        assertFalse(SiblingOverlays.requiresArcGate(FragmentTier.TERRITE, "on", true), "tier 1 has no arc gate");
        for (FragmentTier tier : FragmentTier.values()) {
            assertFalse(SiblingOverlays.requiresArcGate(tier, "off", true),
                    () -> "off mode must never require an arc gate at " + tier);
        }
    }

    @Test
    void unknownModeFallsBackToAuto() {
        assertFalse(SiblingOverlays.requiresArcGate(FragmentTier.ORBITE, "nonsense", false));
        assertTrue(SiblingOverlays.requiresArcGate(FragmentTier.ORBITE, "nonsense", true));
    }
}
