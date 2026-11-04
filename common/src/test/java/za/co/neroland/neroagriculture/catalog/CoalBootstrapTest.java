package za.co.neroland.neroagriculture.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

class CoalBootstrapTest {
    @Test void earlyMachinesHaveCraftingRecipesAndTheResearchBenchDoesNotRequireALectern() {
        for (String name : new String[] {"field_generator", "fragment_extractor", "territe_grow_bed",
                "seed_research_bench", "seed_synthesizer", "prospora_seed"}) {
            JsonObject recipe = resource("recipe/" + name + ".json");
            assertEquals("neroagriculture:" + name, recipe.getAsJsonObject("result").get("id").getAsString());
        }
        JsonObject bench = resource("recipe/seed_research_bench.json");
        assertEquals("minecraft:crafting_table", ingredient(bench.getAsJsonObject("key").get("C")));
        assertFalse(bench.toString().contains("minecraft:lectern"));
        for (String name : new String[] {"field_generator", "fragment_extractor"}) {
            assertFalse(resource("recipe/" + name + ".json").getAsJsonObject("key").toString()
                    .contains("neroagriculture:"));
        }
    }

    @Test void starterPowerUnlocksInTheRecipeBookFromOrdinaryIngredients() {
        JsonObject advancement = resource("advancement/recipes/misc/field_generator.json");
        var criterion = advancement.getAsJsonObject("criteria").getAsJsonObject("has_ingredient");
        assertEquals("minecraft:inventory_changed", criterion.get("trigger").getAsString());
        var ingredients = criterion.getAsJsonObject("conditions").getAsJsonArray("items")
                .get(0).getAsJsonObject().getAsJsonArray("items");
        assertTrue(ingredients.toString().contains("minecraft:iron_ingot"));
        assertTrue(ingredients.toString().contains("minecraft:redstone"));
        assertFalse(ingredients.toString().contains("neroagriculture:"));
        assertEquals("neroagriculture:field_generator", advancement.getAsJsonObject("rewards")
                .getAsJsonArray("recipes").get(0).getAsString());
    }
    @Test void organicResourcesRequireTheRealSampleAndHaveValidDefinitions() {
        for (String name : new String[] {"string", "leather", "slime_ball", "gunpowder", "blaze_rod", "ender_pearl"}) {
            var stream = getClass().getClassLoader().getResourceAsStream(
                    "data/minecraft/neroagriculture/materials/" + name + ".json");
            if (stream == null) throw new AssertionError("Missing material: " + name);
            var json = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            var id = Identifier.parse("minecraft:" + name);
            assertFalse(MaterialCatalog.requirementMet(json, ignored -> false));
            assertTrue(MaterialCatalog.requirementMet(json, id::equals));
            var parsed = MaterialDefinitionParser.parse(id, json);
            assertTrue(parsed.valid(), String.valueOf(parsed.error()));
            assertEquals(id, parsed.definition().input().id());
            assertEquals(id, parsed.definition().output());
            assertTrue(za.co.neroland.neroagriculture.machine.CatalogFabrication.eligible(
                    CatalogSource.DATAPACK, parsed.definition(), false));
        }
    }
    @Test void coalSamplesAreConsistentAcrossTheFirstSeedChain() {
        var coal = BuiltinMaterials.candidates().stream()
                .map(CatalogResolver.Candidate::definition)
                .filter(definition -> definition.id().equals(Identifier.parse("c:coal")))
                .findFirst().orElseThrow();
        assertEquals(MaterialDefinition.InputSelector.Kind.TAG, coal.input().kind());
        assertEquals(Identifier.parse("neroagriculture:coal_samples"), coal.input().id());
        assertEquals(2, coal.yield().minimum());
        assertEquals(8, coal.conversion());
        JsonObject tag = resource("tags/item/coal_samples.json");
        assertTrue(tag.getAsJsonArray("values").toString().contains("minecraft:coal"));
        assertTrue(tag.getAsJsonArray("values").toString().contains("#c:ores/coal"));
        for (String operation : new String[] {"extracting", "researching", "synthesizing"}) {
            JsonObject recipe = resource("recipe/" + operation + "/coal.json");
            assertEquals("#neroagriculture:coal_samples", ingredient(recipe.get("ingredient")));
            assertEquals("c:coal", recipe.get("material").getAsString());
            assertFalse(ingredient(recipe.get("ingredient")).contains("iron"));
        }
    }

    @Test void everyBuiltInMaterialHasMatchingFabricationRecipes() {
        for (var candidate : BuiltinMaterials.candidates()) {
            var definition = candidate.definition();
            String name = definition.id().getPath();
            String ingredient = (definition.input().kind() == MaterialDefinition.InputSelector.Kind.TAG ? "#" : "")
                    + definition.input().id();
            for (String operation : new String[] {"extracting", "researching", "synthesizing"}) {
                JsonObject recipe = resource("recipe/" + operation + "/" + name + ".json");
                assertEquals(definition.id().toString(), recipe.get("material").getAsString());
                assertEquals(ingredient, ingredient(recipe.get("ingredient")));
                if (recipe.has("family")) {
                    assertEquals(definition.tier().name().toLowerCase(java.util.Locale.ROOT),
                            recipe.get("family").getAsString());
                } else {
                    assertEquals("nerolandcore", definition.id().getNamespace());
                }
            }
        }
    }

    @Test void optionalPlanetMaterialsRequireTheItemAndTheCorrectWorld() {
        for (String[] entry : new String[][] {{"cindrite", "cindara"}, {"glacite", "glacira"},
                {"xertz_quartz", "greenxertz"}, {"raw_hematite", "mars"}, {"voidglass", "nyxara"}}) {
            var stream = getClass().getClassLoader().getResourceAsStream(
                    "data/nerospace/neroagriculture/materials/" + entry[0] + ".json");
            if (stream == null) throw new AssertionError("Missing material: " + entry[0]);
            var json = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            assertFalse(MaterialCatalog.requirementMet(json, id -> false));
            Identifier material = Identifier.parse("nerospace:" + entry[0]);
            assertFalse(MaterialCatalog.requirementMet(json, Identifier.parse("minecraft:coal")::equals));
            assertTrue(MaterialCatalog.requirementMet(json, material::equals));
            var parsed = MaterialDefinitionParser.parse(material, json);
            assertTrue(parsed.valid());
            assertEquals(Identifier.parse("nerospace:" + entry[1]), parsed.definition().worldRestriction().dimension());
            assertTrue(parsed.definition().conversion() >= 16);
            assertEquals(material, parsed.definition().output());
            assertTrue(za.co.neroland.neroagriculture.machine.CatalogFabrication.eligible(
                    CatalogSource.DATAPACK, parsed.definition(), false));
        }
    }

    @Test void optionalCreatureMaterialsNeedTheirSourceItem() {
        for (String name : new String[] {"stalker_hide", "stalker_sinew"}) {
            var stream = getClass().getClassLoader().getResourceAsStream(
                    "data/nerocreatures/neroagriculture/materials/" + name + ".json");
            if (stream == null) throw new AssertionError("Missing material: " + name);
            var json = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            assertFalse(MaterialCatalog.requirementMet(json, id -> false));
            assertTrue(MaterialCatalog.requirementMet(json, id -> true));
            var result = MaterialDefinitionParser.parse(Identifier.parse("nerocreatures:" + name), json);
            assertTrue(result.valid());
            assertEquals(20, result.definition().conversion());
        }
    }

    @Test void newPlanetMaterialsKeepTheirTierAppearanceAndWorldRestriction() {
        for (String[] entry : new String[][] {
                {"raw_hematite", "mars", "ORBITE", "metal"},
                {"voidglass", "nyxara", "VOIDITE", "crystal"}}) {
            var stream = getClass().getClassLoader().getResourceAsStream(
                    "data/nerospace/neroagriculture/materials/" + entry[0] + ".json");
            if (stream == null) throw new AssertionError("Missing material: " + entry[0]);
            var json = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            var material = Identifier.parse("nerospace:" + entry[0]);
            var result = MaterialDefinitionParser.parse(material, json);
            assertTrue(result.valid());
            var definition = result.definition();
            assertEquals(entry[2], definition.tier().name());
            assertEquals(entry[3], za.co.neroland.neroagriculture.crop.CropVisuals.name(definition.visualFamily()));
            var requiredWorld = definition.worldRestriction().dimension();
            assertTrue(MaterialWorldRules.allows(requiredWorld, Identifier.parse("nerospace:" + entry[1])));
            assertFalse(MaterialWorldRules.allows(requiredWorld, Identifier.parse("minecraft:overworld")));
        }
    }

    private static String ingredient(com.google.gson.JsonElement value) {
        if (value.isJsonPrimitive()) return value.getAsString();
        var object = value.getAsJsonObject();
        return object.has("tag") ? "#" + object.get("tag").getAsString() : object.get("item").getAsString();
    }

    private static JsonObject resource(String path) {
        var stream = CoalBootstrapTest.class.getClassLoader().getResourceAsStream("data/neroagriculture/" + path);
        if (stream == null) throw new AssertionError("Missing resource: " + path);
        return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
    }
}
