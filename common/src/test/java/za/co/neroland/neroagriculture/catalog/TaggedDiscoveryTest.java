package za.co.neroland.neroagriculture.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.resources.Identifier;

import org.junit.jupiter.api.Test;

import za.co.neroland.neroagriculture.content.FragmentTier;

/** Boundary tests for Stage-1 auto-generation: tier heuristic, colour resolution and colour override. */
class TaggedDiscoveryTest {

    @Test
    void knownMaterialsMapToCuratedTiers() {
        assertEquals(FragmentTier.TERRITE, TierHeuristic.assign("coal", "ores"));
        assertEquals(FragmentTier.FORGITE, TierHeuristic.assign("iron", "ingots"));
        assertEquals(FragmentTier.ORBITE, TierHeuristic.assign("diamond", "gems"));
        assertEquals(FragmentTier.COLONITE, TierHeuristic.assign("netherite", "ingots"));
        assertEquals(FragmentTier.VOIDITE, TierHeuristic.assign("echo_shard", "gems"));
    }

    @Test
    void categoryDefaultsAndKeywordsBumpUnknownMaterials() {
        // Unknown/uncategorised materials fall back to the configured discovery.default_tier (orbite by
        // default); unknown gems stay orbite regardless.
        assertEquals(FragmentTier.ORBITE, TierHeuristic.assign("mythril", "ingots"));
        assertEquals(FragmentTier.ORBITE, TierHeuristic.assign("ruby", "gems"));
        // The pure overload pins the fallback: gems ignore it, everything else uses it.
        assertEquals(FragmentTier.TERRITE, TierHeuristic.assign("mythril", "ingots", FragmentTier.TERRITE));
        assertEquals(FragmentTier.ORBITE, TierHeuristic.assign("ruby", "gems", FragmentTier.TERRITE));
        // Rarity keywords pull a material up but never below the category default.
        assertEquals(FragmentTier.VOIDITE, TierHeuristic.assign("void_crystal", "ingots"));
        assertEquals(FragmentTier.VOIDITE, TierHeuristic.assign("starsteel", "ingots"));
        assertTrue(TierHeuristic.assign("titanium", "ingots").ordinal() >= FragmentTier.FORGITE.ordinal());
    }

    @Test
    void colourResolutionIsCuratedThenStableAndInRange() {
        assertEquals(0xD8D8D8, MaterialColors.resolve("iron"));
        assertEquals(0x55D6C8, MaterialColors.resolve("diamond"));
        int derived = MaterialColors.resolve("unobtainium");
        assertEquals(derived, MaterialColors.resolve("unobtainium"), "derived colour must be stable");
        assertEquals(0, derived & 0xFF000000, "colour must be 24-bit RGB");
    }

    @Test
    void colourOverrideParsesHexAndApplies() {
        CatalogConfigParser.Parsed parsed = CatalogConfigParser.parse("",
                "test:iron|tier=forgite|color=#123456");
        assertTrue(parsed.errors().isEmpty(), () -> "unexpected errors: " + parsed.errors());
        MaterialOverride override = parsed.overrides().get(Identifier.parse("test:iron"));
        assertNotNull(override);
        assertEquals(0x123456, override.color());
    }

    @Test
    void badColourOverrideIsReportedNotThrown() {
        CatalogConfigParser.Parsed parsed = CatalogConfigParser.parse("", "test:iron|color=notacolour");
        assertEquals(1, parsed.errors().size());
        assertTrue(parsed.overrides().isEmpty());
    }
}
