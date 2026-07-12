package za.co.neroland.neroagriculture.environment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.resources.Identifier;

import org.junit.jupiter.api.Test;

import za.co.neroland.neroagriculture.content.EssenceFamily;
import za.co.neroland.neroagriculture.environment.CropClimate.Result;

class EnvironmentTest {
    private static final int ORBITAL_THRESHOLD = EssenceFamily.ORBITAL.ordinal();
    private static final EnvironmentProfile HOSTILE = new EnvironmentProfile(Temperature.HOT, false, true);

    @Test
    void sealedAlwaysGrowsRegardlessOfTierOrWorld() {
        assertEquals(Result.OK, CropClimate.evaluate(HOSTILE, true, EssenceFamily.DEEPVOID.ordinal(), ORBITAL_THRESHOLD));
        assertEquals(Result.OK, CropClimate.evaluate(EnvironmentProfile.HABITABLE, true, 0, ORBITAL_THRESHOLD));
    }

    @Test
    void lowTierGrowsOpenAirOnlyWhenHabitable() {
        assertEquals(Result.OK, CropClimate.evaluate(EnvironmentProfile.HABITABLE, false,
                EssenceFamily.TERRAN.ordinal(), ORBITAL_THRESHOLD));
        assertEquals(Result.HOSTILE_ENVIRONMENT, CropClimate.evaluate(HOSTILE, false,
                EssenceFamily.TERRAN.ordinal(), ORBITAL_THRESHOLD));
    }

    @Test
    void highTierAlwaysNeedsSealingEvenInHabitableWorlds() {
        assertEquals(Result.NEEDS_GREENHOUSE, CropClimate.evaluate(EnvironmentProfile.HABITABLE, false,
                EssenceFamily.ORBITAL.ordinal(), ORBITAL_THRESHOLD));
    }

    @Test
    void thresholdParsingDefaultsToOrbital() {
        assertEquals(EssenceFamily.INDUSTRIAL.ordinal(), CropClimate.thresholdOrdinal("industrial"));
        assertEquals(EssenceFamily.ORBITAL.ordinal(), CropClimate.thresholdOrdinal("nonsense"));
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
