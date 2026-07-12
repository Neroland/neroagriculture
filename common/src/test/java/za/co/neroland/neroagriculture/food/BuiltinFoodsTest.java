package za.co.neroland.neroagriculture.food;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumSet;
import java.util.List;

import org.junit.jupiter.api.Test;

import za.co.neroland.neroagriculture.food.FoodDefinition.Kind;

/** Gate 7 coverage: every effect category and planet theme has a playable crop, and caps hold. */
class BuiltinFoodsTest {
    private static final List<FoodDefinition> ALL = BuiltinFoods.definitions();

    @Test
    void everySignatureEffectCategoryHasACrop() {
        EnumSet<EffectCategory> required = EnumSet.of(EffectCategory.NIGHT_VISION, EffectCategory.MINING_HASTE,
                EffectCategory.FIRE_RESISTANCE, EffectCategory.LOW_GRAVITY_ADAPTATION,
                EffectCategory.OXYGEN_EFFICIENCY, EffectCategory.FREEZE_IMMUNITY);
        EnumSet<EffectCategory> present = EnumSet.noneOf(EffectCategory.class);
        for (FoodDefinition definition : ALL) present.add(definition.effect());
        for (EffectCategory category : required) {
            assertTrue(present.contains(category), "missing a crop for effect category " + category);
        }
    }

    @Test
    void everyPlanetThemeAndEarthArePresent() {
        EnumSet<PlanetTheme> present = EnumSet.noneOf(PlanetTheme.class);
        for (FoodDefinition definition : ALL) present.add(definition.theme());
        for (PlanetTheme theme : PlanetTheme.values()) {
            assertTrue(present.contains(theme), "missing a crop for planet theme " + theme);
        }
    }

    @Test
    void bothNaturalAndDerivedAlienStrainsShip() {
        boolean natural = ALL.stream().anyMatch(d -> d.kind() == Kind.ALIEN && d.natural());
        boolean derived = ALL.stream().anyMatch(d -> d.kind() == Kind.ALIEN && !d.natural());
        assertTrue(natural, "at least one exploration-only natural alien strain must exist");
        assertTrue(derived, "at least one researchable derived alien strain must exist");
    }

    @Test
    void everyEffectFoodStaysWithinItsCaps() {
        for (FoodDefinition definition : ALL) {
            assertTrue(definition.effectiveAmplifier() <= definition.potencyCap(), definition.id() + " amplifier cap");
            assertTrue(definition.effectiveDurationTicks() <= definition.durationCap(), definition.id() + " duration cap");
            assertTrue(definition.nutrition() > 0 && definition.nutrition() <= FoodDefinition.MAX_NUTRITION,
                    definition.id() + " nutrition ladder");
        }
    }

    @Test
    void naturalAlienStrainsAreNotSynthesizable() {
        assertFalse(ALL.stream().filter(d -> d.kind() == Kind.ALIEN && d.natural())
                .allMatch(FoodDefinition::synthesizable), "natural alien strains must not be synthesizable");
    }
}
