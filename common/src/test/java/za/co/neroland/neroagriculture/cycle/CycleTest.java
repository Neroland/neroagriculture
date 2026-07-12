package za.co.neroland.neroagriculture.cycle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import net.minecraft.resources.Identifier;

import org.junit.jupiter.api.Test;

class CycleTest {
    private static CycleProfile fourSeasons() {
        List<CycleProfile.Phase> phases = List.of(
                new CycleProfile.Phase("spring", new CycleModifier(1.25F, 1.0F, 1.0F)),
                new CycleProfile.Phase("summer", new CycleModifier(1.1F, 1.2F, 1.0F)),
                new CycleProfile.Phase("autumn", new CycleModifier(1.0F, 1.1F, 1.0F)),
                new CycleProfile.Phase("winter", new CycleModifier(0.7F, 0.9F, 1.0F)));
        return new CycleProfile(Identifier.parse("minecraft:overworld"), 96_000L, 0L, phases);
    }

    @Test
    void modifiersAreClampedIntoTheSafeRange() {
        CycleModifier extreme = new CycleModifier(100.0F, -5.0F, 0.0F);
        assertEquals(CycleModifier.MAX, extreme.growth(), "excessive growth clamps to the maximum");
        assertEquals(CycleModifier.MIN, extreme.yield(), "non-positive clamps to the minimum");
        assertEquals(CycleModifier.MIN, extreme.environment());
    }

    @Test
    void combiningModifiersStaysBounded() {
        CycleModifier combined = new CycleModifier(3.0F, 3.0F, 3.0F).combine(new CycleModifier(3.0F, 3.0F, 3.0F));
        assertEquals(CycleModifier.MAX, combined.growth(), "9x is clamped back to the maximum");
        CycleModifier identity = CycleModifier.IDENTITY.combine(CycleModifier.IDENTITY);
        assertEquals(1.0F, identity.growth());
    }

    @Test
    void phaseIsADeterministicFunctionOfTimeAndRepeatsEveryPeriod() {
        CycleProfile profile = fourSeasons();
        assertEquals(0, profile.phaseIndex(0));
        assertEquals(1, profile.phaseIndex(24_000));
        assertEquals(3, profile.phaseIndex(95_999));
        assertEquals(0, profile.phaseIndex(96_000), "the cycle wraps after one period");
        assertEquals(1, profile.phaseIndex(96_000 + 24_000));
        // A time jump forward by a whole period lands on the identical phase — no exploit from /time.
        assertEquals(profile.modifierAt(12_345), profile.modifierAt(12_345 + 96_000));
    }

    @Test
    void forecastReportsTheNextPhaseBoundary() {
        CycleProfile profile = fourSeasons();
        assertEquals(24_000L, profile.ticksUntilNextPhase(0));
        assertEquals(1L, profile.ticksUntilNextPhase(23_999));
        assertEquals("summer", profile.nextPhase(0).displayKey());
        assertEquals("spring", profile.nextPhase(72_000).displayKey(), "next wraps around to the first phase");
    }

    @Test
    void aProfileOfIdentityPhasesNeverChangesOutput() {
        CycleProfile flat = new CycleProfile(Identifier.parse("test:flat"), 100L, 0L,
                List.of(new CycleProfile.Phase("a", CycleModifier.IDENTITY)));
        for (long t = 0; t < 500; t += 37) {
            assertTrue(flat.modifierAt(t).growth() == 1.0F && flat.modifierAt(t).yield() == 1.0F);
        }
    }
}
