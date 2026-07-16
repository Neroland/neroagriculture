package za.co.neroland.neroagriculture.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gson.JsonParser;

import net.minecraft.resources.Identifier;

import org.junit.jupiter.api.Test;

import za.co.neroland.neroagriculture.catalog.CatalogResolver.Candidate;
import za.co.neroland.neroagriculture.content.FragmentTier;

class CatalogResolverTest {
    @Test
    void precedenceAndConfigOverlayAreDeterministic() {
        Identifier id = Identifier.parse("test:iron");
        List<Candidate> candidates = List.of(candidate(id, CatalogSource.ORE_TAG, FragmentTier.ORBITE, "tag"),
                candidate(id, CatalogSource.BUILTIN, FragmentTier.FORGITE, "builtin"),
                candidate(id, CatalogSource.METEOR, FragmentTier.COLONITE, "meteor"));
        MaterialOverride override = new MaterialOverride(FragmentTier.TERRITE, null, true, null, 3, true, null);
        ResolvedCatalog catalog = CatalogResolver.resolve(candidates, Set.of(), Map.of(id, override), 16, List.of());
        ResolvedMaterial resolved = catalog.exposed().get(id);
        assertEquals(CatalogSource.CONFIG, resolved.source());
        assertEquals(FragmentTier.TERRITE, resolved.definition().tier());
        assertNull(resolved.definition().gate());
        assertEquals(3, resolved.definition().conversion());
        assertTrue(catalog.errors().stream().anyMatch(error -> error.contains("shadowed")));

        ResolvedCatalog datapackWins = CatalogResolver.resolve(List.of(
                candidate(id, CatalogSource.DATAPACK, FragmentTier.VOIDITE, "pack"),
                candidate(id, CatalogSource.ORE_TAG, FragmentTier.ORBITE, "tag")),
                Set.of(), Map.of(id, override), 16, List.of());
        assertEquals(CatalogSource.DATAPACK, datapackWins.exposed().get(id).source());
        assertEquals(FragmentTier.VOIDITE, datapackWins.exposed().get(id).definition().tier());
    }

    @Test
    void blacklistAndCapApplyBeforeExposureWithoutDeletingDefinitions() {
        Identifier a = Identifier.parse("test:a");
        Identifier b = Identifier.parse("test:b");
        Identifier c = Identifier.parse("test:c");
        ResolvedCatalog catalog = CatalogResolver.resolve(List.of(candidate(c, CatalogSource.ORE_TAG, FragmentTier.ORBITE, "c"),
                candidate(a, CatalogSource.ORE_TAG, FragmentTier.ORBITE, "a"),
                candidate(b, CatalogSource.ORE_TAG, FragmentTier.ORBITE, "b")), Set.of(a), Map.of(), 1, List.of());
        assertEquals(List.of(a, b, c), new ArrayList<>(catalog.all().keySet()));
        assertEquals(List.of(b), new ArrayList<>(catalog.exposed().keySet()));
        assertEquals(ResolvedCatalog.Status.DISABLED, catalog.lookup(a).status());
        assertEquals(ResolvedCatalog.Status.DISABLED, catalog.lookup(c).status());
        assertEquals(ResolvedCatalog.Status.UNKNOWN, catalog.lookup(Identifier.parse("test:missing")).status());
    }

    @Test
    void parserRejectsActionableInvalidInputAndAppliesGateFallback() {
        Identifier id = Identifier.parse("test:copper");
        var valid = JsonParser.parseString("""
                {"input":{"item":"minecraft:copper_ingot"},"output":"minecraft:copper_ingot",
                 "tier":"forgite","yield":{"minimum":1,"maximum":3,"ramp_harvests":20},
                 "conversion":8,"display_key":"material.test.copper","color":"#C07050"}
                """).getAsJsonObject();
        MaterialDefinitionParser.Result result = MaterialDefinitionParser.parse(id, valid);
        assertTrue(result.valid());
        assertNull(result.definition().gate(), "no hard gates by default");

        var invalid = JsonParser.parseString("""
                {"input":{"item":"bad id","tag":"c:ores/copper"},"output":"minecraft:copper_ingot",
                 "tier":"early","yield":{"minimum":5,"maximum":1,"ramp_harvests":20},
                 "conversion":0,"display_key":"","color":"oops"}
                """).getAsJsonObject();
        MaterialDefinitionParser.Result failed = MaterialDefinitionParser.parse(id, invalid);
        assertFalse(failed.valid());
        assertTrue(failed.error().contains("input must contain exactly one"));
    }

    @Test
    void configParserReportsBadEntriesAndKeepsGoodOnes() {
        CatalogConfigParser.Parsed parsed = CatalogConfigParser.parse("test:blocked,bad id",
                "test:good|tier=colonite|yield=1:6:80|conversion=20;test:bad|wat=nope");
        assertTrue(parsed.blacklist().contains(Identifier.parse("test:blocked")));
        assertEquals(FragmentTier.COLONITE, parsed.overrides().get(Identifier.parse("test:good")).tier());
        assertEquals(2, parsed.errors().size());
    }

    private static Candidate candidate(Identifier id, CatalogSource source, FragmentTier tier, String detail) {
        return new Candidate(new MaterialDefinition(id,
                new MaterialDefinition.InputSelector(MaterialDefinition.InputSelector.Kind.ITEM, Identifier.parse("minecraft:iron_ingot")),
                Identifier.parse("minecraft:iron_ingot"), tier, MaterialDefinitionParser.defaultGate(tier),
                new MaterialDefinition.Yield(1, 4, 32), 8, "material.test." + id.getPath(), 0xA0A0A0, true, null), source, detail);
    }
}
