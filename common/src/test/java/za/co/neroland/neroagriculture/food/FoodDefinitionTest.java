package za.co.neroland.neroagriculture.food;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.resources.Identifier;

import org.junit.jupiter.api.Test;

import za.co.neroland.neroagriculture.content.EssenceFamily;
import za.co.neroland.neroagriculture.food.FoodDefinition.Kind;

class FoodDefinitionTest {
    @Test
    void amplifierAndDurationAreClampedToCaps() {
        FoodDefinition definition = new FoodDefinition(Identifier.parse("neroagriculture:food/test"), Kind.FOOD, false,
                EffectCategory.MINING_HASTE, 9, 24_000, 2, 6000, 6, 0.6F, EssenceFamily.ORBITAL, PlanetTheme.CINDARA,
                true, null, "food.neroagriculture.test", 0x808080, 0);
        assertEquals(2, definition.effectiveAmplifier(), "amplifier must be clamped to the cap");
        assertEquals(6000, definition.effectiveDurationTicks(), "duration must be clamped to the cap");
        assertTrue(definition.hasEffect());
    }

    @Test
    void naturalAlienStrainsAreNotSynthesizable() {
        FoodDefinition natural = new FoodDefinition(Identifier.parse("neroagriculture:alien/wild"), Kind.ALIEN, true,
                EffectCategory.NONE, 0, 0, 0, 0, 6, 0.5F, EssenceFamily.ORBITAL, PlanetTheme.EARTH, true, null,
                "alien.neroagriculture.wild", 0x808080, 0);
        FoodDefinition derived = new FoodDefinition(Identifier.parse("neroagriculture:alien/bred"), Kind.ALIEN, false,
                EffectCategory.NONE, 0, 0, 0, 0, 6, 0.5F, EssenceFamily.ORBITAL, PlanetTheme.EARTH, true, null,
                "alien.neroagriculture.bred", 0x808080, 0);
        assertFalse(natural.synthesizable(), "natural alien strains must be found, never synthesized");
        assertTrue(derived.synthesizable(), "derived alien strains may be synthesized");
    }

    @Test
    void noEffectMeansPureNutrition() {
        FoodDefinition food = new FoodDefinition(Identifier.parse("neroagriculture:food/plain"), Kind.FOOD, false,
                EffectCategory.NONE, 0, 0, 0, 0, 8, 0.8F, EssenceFamily.TERRAN, PlanetTheme.EARTH, true, null,
                "food.neroagriculture.plain", 0x808080, 0);
        assertFalse(food.hasEffect());
        assertTrue(food.synthesizable());
    }
}
