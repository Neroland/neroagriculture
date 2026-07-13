package za.co.neroland.neroagriculture.environment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.resources.Identifier;

import org.junit.jupiter.api.Test;

import za.co.neroland.neroagriculture.content.FragmentTier;
import za.co.neroland.neroagriculture.environment.CropClimate.Result;

class EnvironmentTest {
    private static final int ORBITE_THRESHOLD = FragmentTier.ORBITE.ordinal();
    private static final EnvironmentProfile HOSTILE = new EnvironmentProfile(Temperature.HOT, false, true);

    @Test
    void sealedAlwaysGrowsRegardlessOfTierOrWorld() {
        assertEquals(Result.OK, CropClimate.evaluate(HOSTILE, true, FragmentTier.VOIDITE.ordinal(), ORBITE_THRESHOLD));
        assertEquals(Result.OK, CropClimate.evaluate(EnvironmentProfile.HABITABLE, true, 0, ORBITE_THRESHOLD));
    }

    @Test
    void lowTierGrowsOpenAirOnlyWhenHabitable() {
        assertEquals(Result.OK, CropClimate.evaluate(EnvironmentProfile.HABITABLE, false,
                FragmentTier.TERRITE.ordinal(), ORBITE_THRESHOLD));
        assertEquals(Result.HOSTILE_ENVIRONMENT, CropClimate.evaluate(HOSTILE, false,
                FragmentTier.TERRITE.ordinal(), ORBITE_THRESHOLD));
    }

    @Test
    void highTierAlwaysNeedsSealingEvenInHabitableWorlds() {
        assertEquals(Result.NEEDS_GREENHOUSE, CropClimate.evaluate(EnvironmentProfile.HABITABLE, false,
                FragmentTier.ORBITE.ordinal(), ORBITE_THRESHOLD));
    }

    @Test
    void thresholdParsingDefaultsToOrbite() {
        assertEquals(FragmentTier.FORGITE.ordinal(), CropClimate.thresholdOrdinal("forgite"));
        assertEquals(FragmentTier.ORBITE.ordinal(), CropClimate.thresholdOrdinal("nonsense"));
    }

    @Test
    void builtInDimensionDefaultsClassifyVanillaWorlds() {
        assertTrue(DimensionEnvironments.profileFor(null, Identifier.parse("minecraft:overworld")).habitable());
        assertFalse(DimensionEnvironments.profileFor(null, Identifier.parse("minecraft:the_nether")).habitable());
        assertFalse(DimensionEnvironments.profileFor(null, Identifier.parse("minecraft:the_end")).habitable());
        // Unclassified dimensions must default to habitable so third-party worlds are not broken.
        assertTrue(DimensionEnvironments.profileFor(null, Identifier.parse("othermod:garden")).habitable());
    }
}
