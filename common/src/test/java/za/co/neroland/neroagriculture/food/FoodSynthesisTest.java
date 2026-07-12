package za.co.neroland.neroagriculture.food;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.resources.Identifier;

import org.junit.jupiter.api.Test;

import za.co.neroland.neroagriculture.content.EssenceFamily;
import za.co.neroland.neroagriculture.food.FoodDefinition.Kind;

class FoodSynthesisTest {
    private static FoodDefinition alien(boolean natural) {
        return new FoodDefinition(Identifier.parse("neroagriculture:alien/x"), Kind.ALIEN, natural,
                EffectCategory.NONE, 0, 0, 0, 0, 6, 0.5F, EssenceFamily.ORBITAL, PlanetTheme.EARTH, true, null,
                "alien.neroagriculture.x", 0x808080);
    }

    @Test
    void undiscoveredNaturalStrainsAreRejectedEvenWhenResearchFlagIsTrue() {
        assertFalse(FoodSynthesis.canSynthesize(alien(true), true));
        assertFalse(FoodSynthesis.canSynthesize(alien(true), false));
    }

    @Test
    void derivedStrainsRequireResearchThenSynthesize() {
        assertFalse(FoodSynthesis.canSynthesize(alien(false), false), "derived strains still need research");
        assertTrue(FoodSynthesis.canSynthesize(alien(false), true));
    }
}
