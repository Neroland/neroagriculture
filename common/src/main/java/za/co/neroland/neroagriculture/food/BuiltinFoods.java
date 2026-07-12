package za.co.neroland.neroagriculture.food;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.resources.Identifier;

import za.co.neroland.neroagriculture.content.EssenceFamily;
import za.co.neroland.neroagriculture.food.FoodDefinition.Kind;

/**
 * Conservative, standalone-complete built-in food and alien species. Covers all six signature-effect
 * categories and the three Nerospace planet themes plus Earth. Datapacks may add or replace entries by id.
 */
public final class BuiltinFoods {
    private BuiltinFoods() { }

    public static List<FoodDefinition> definitions() {
        List<FoodDefinition> out = new ArrayList<>();

        // Earth — nutrition baseline and an early night-vision food.
        food(out, "earth_grain_loaf", EffectCategory.NONE, 0, 0, EssenceFamily.TERRAN, PlanetTheme.EARTH,
                6, 0.6F, 0, 0, 0xD9B44A);
        food(out, "earth_sunfruit", EffectCategory.NIGHT_VISION, 0, 2400, EssenceFamily.TERRAN, PlanetTheme.EARTH,
                4, 0.4F, 0, 6000, 0xE8C34A);

        // Greenxertz — lush low-gravity world: low-gravity adaptation and oxygen efficiency.
        food(out, "greenxertz_driftmelon", EffectCategory.LOW_GRAVITY_ADAPTATION, 0, 3600, EssenceFamily.INDUSTRIAL,
                PlanetTheme.GREENXERTZ, 6, 0.6F, 1, 6000, 0x7FD68A);
        food(out, "greenxertz_lungmoss", EffectCategory.OXYGEN_EFFICIENCY, 0, 3600, EssenceFamily.INDUSTRIAL,
                PlanetTheme.GREENXERTZ, 5, 0.5F, 1, 9600, 0x4FBF6E);

        // Cindara — volcanic world: fire resistance and mining haste.
        food(out, "cindara_emberroot", EffectCategory.FIRE_RESISTANCE, 0, 3600, EssenceFamily.ORBITAL,
                PlanetTheme.CINDARA, 6, 0.7F, 0, 9600, 0xD5622A);
        food(out, "cindara_magmagourd", EffectCategory.MINING_HASTE, 0, 2400, EssenceFamily.ORBITAL,
                PlanetTheme.CINDARA, 7, 0.6F, 2, 6000, 0xE0853A);

        // Glacira — frozen dark world: freeze immunity and night vision.
        food(out, "glacira_frostberry", EffectCategory.FREEZE_IMMUNITY, 0, 3600, EssenceFamily.ORBITAL,
                PlanetTheme.GLACIRA, 4, 0.4F, 0, 9600, 0x8FC8E8);
        food(out, "glacira_glowcap", EffectCategory.NIGHT_VISION, 0, 4800, EssenceFamily.COLONIAL,
                PlanetTheme.GLACIRA, 5, 0.5F, 0, 9600, 0xB6E0F0);

        // Alien natural strains — found, never synthesized until a derived strain is researched.
        alien(out, "voidchorus", true, EffectCategory.NONE, 0, 0, EssenceFamily.ORBITAL, PlanetTheme.EARTH,
                6, 0.5F, 0, 0, 0x9B4FD1);
        alien(out, "greenxertz_sporepod", true, EffectCategory.OXYGEN_EFFICIENCY, 0, 3600, EssenceFamily.COLONIAL,
                PlanetTheme.GREENXERTZ, 5, 0.5F, 1, 9600, 0x6ED0A0);
        // Alien derived strain — researched/bred, so the Synthesizer accepts it.
        alien(out, "hybrid_gloomvine", false, EffectCategory.NIGHT_VISION, 0, 4800, EssenceFamily.COLONIAL,
                PlanetTheme.EARTH, 5, 0.5F, 0, 9600, 0x7A6FC0);

        // Oxygen flora — low-nutrition greenhouse life-support plants that contribute oxygen.
        oxygenFlora(out, "earth_algae", EssenceFamily.TERRAN, PlanetTheme.EARTH, 1, 3, 0x3FA66A);
        oxygenFlora(out, "greenxertz_oxyvine", EssenceFamily.INDUSTRIAL, PlanetTheme.GREENXERTZ, 2, 5, 0x5FE08A);

        return List.copyOf(out);
    }

    private static void food(List<FoodDefinition> out, String path, EffectCategory effect, int amplifier,
            int durationTicks, EssenceFamily tier, PlanetTheme theme, int nutrition, float saturation,
            int potencyCap, int durationCap, int color) {
        Identifier id = Identifier.fromNamespaceAndPath("neroagriculture", "food/" + path);
        out.add(new FoodDefinition(id, Kind.FOOD, false, effect, amplifier, durationTicks, potencyCap, durationCap,
                nutrition, saturation, tier, theme, true, null, "food.neroagriculture." + path, color, 0));
    }

    private static void alien(List<FoodDefinition> out, String path, boolean natural, EffectCategory effect,
            int amplifier, int durationTicks, EssenceFamily tier, PlanetTheme theme, int nutrition, float saturation,
            int potencyCap, int durationCap, int color) {
        Identifier id = Identifier.fromNamespaceAndPath("neroagriculture", "alien/" + path);
        out.add(new FoodDefinition(id, Kind.ALIEN, natural, effect, amplifier, durationTicks, potencyCap, durationCap,
                nutrition, saturation, tier, theme, true, null, "alien.neroagriculture." + path, color, 0));
    }

    private static void oxygenFlora(List<FoodDefinition> out, String path, EssenceFamily tier, PlanetTheme theme,
            int nutrition, int oxygenProduction, int color) {
        Identifier id = Identifier.fromNamespaceAndPath("neroagriculture", "food/" + path);
        out.add(new FoodDefinition(id, Kind.FOOD, false, EffectCategory.NONE, 0, 0, 0, 0, nutrition, 0.2F, tier,
                theme, true, null, "food.neroagriculture." + path, color, oxygenProduction));
    }
}
