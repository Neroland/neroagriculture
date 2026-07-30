package za.co.neroland.neroagriculture.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.Identifier;

import org.junit.jupiter.api.Test;

import za.co.neroland.neroagriculture.content.FragmentTier;
import za.co.neroland.neroagriculture.content.MaterialNames;
import za.co.neroland.neroagriculture.network.MaterialCatalogSyncPayload.Entry;

class SeedCompatibilityTest {
    /**
     * The only keys this "language" knows. Everything else has to come out of the shared fallback, which
     * is the normal case in a real game: tag-discovered and meteor materials mint their display key at
     * runtime, so no lang file can ever carry it.
     */
    private static final Map<String, String> TRANSLATIONS = Map.of(
            "material.test.aluminium", "aluminium",
            "material.test.star", "Nether Star",
            "material.test.echo", "Echo Shard");

    @Test
    void bedTierSplitsTheCatalogueIntoWhatGrowsNowAndWhatNeedsABetterBed() {
        List<SeedCompatibility.Row> rows = SeedCompatibility.rows(catalogue(), FragmentTier.ORBITE, this::name);
        assertEquals(6, rows.size());
        assertEquals(4, SeedCompatibility.acceptedCount(rows));
        for (SeedCompatibility.Row row : rows) {
            assertEquals(row.tier().ordinal() <= FragmentTier.ORBITE.ordinal(), row.accepted(),
                    () -> row.id() + " partitioned against the bed tier rule");
        }
        // Locked rows still carry their own tier so the panel can show what to upgrade to.
        assertEquals(FragmentTier.COLONITE, rows.get(4).tier());
        assertEquals(FragmentTier.VOIDITE, rows.get(5).tier());
    }

    @Test
    void rowsAreAcceptedFirstThenByTierThenByDisplayName() {
        List<SeedCompatibility.Row> rows = SeedCompatibility.rows(catalogue(), FragmentTier.FORGITE, this::name);
        assertEquals(List.of("Coal", "aluminium", "Iron", "Diamond", "Nether Star", "Echo Shard"),
                rows.stream().map(SeedCompatibility.Row::name).toList());
        assertTrue(rows.get(0).accepted());
        assertTrue(rows.get(2).accepted());
        assertFalse(rows.get(3).accepted());
    }

    @Test
    void aTierlessMachineAcceptsEverythingAndAnEmptyCatalogueYieldsNoRows() {
        List<SeedCompatibility.Row> rows = SeedCompatibility.rows(catalogue(), null, this::name);
        assertEquals(6, SeedCompatibility.acceptedCount(rows));
        assertEquals(0, SeedCompatibility.rows(List.of(), FragmentTier.VOIDITE, this::name).size());
        assertEquals(0, SeedCompatibility.acceptedCount(List.of()));
    }

    @Test
    void scrollIsClampedToTheLastPageAndNeverAboveTheFirst() {
        assertEquals(0, SeedCompatibility.clampScroll(-4, 20, 6));
        assertEquals(5, SeedCompatibility.clampScroll(9, 11, 6));
        assertEquals(3, SeedCompatibility.clampScroll(3, 20, 6));
        // Fewer lines than rows on screen: nothing to scroll.
        assertEquals(0, SeedCompatibility.clampScroll(4, 3, 6));
        assertEquals(0, SeedCompatibility.clampScroll(4, 0, 0));
    }

    @Test
    void tierTagsAreThreeLettersAndUnique() {
        assertEquals(List.of("TER", "FOR", "ORB", "COL", "VOI"),
                java.util.Arrays.stream(FragmentTier.values()).map(SeedCompatibility::tag).toList());
    }

    @Test
    void materialsWithNoTranslationFallBackToTheTitleCasedLeafPathInsteadOfTheRawKey() {
        // The bug this locks: the panels used to render "material.c.iron" verbatim, because no lang file
        // carries the key and none ever can for a runtime-discovered material.
        assertEquals("Iron", MaterialNames.display(Identifier.parse("c:iron"), "material.c.iron", key -> false)
                .getString());
        assertEquals("Nether Star", MaterialNames.display(Identifier.parse("c:nether_star"),
                "material.c.nether_star", key -> false).getString());
        // Namespaced sub-paths fall back to the leaf, matching the item tooltips.
        assertEquals("Earth Algae", MaterialNames.display(Identifier.parse("neroagriculture:food/earth_algae"),
                "food.neroagriculture.earth_algae", key -> false).getString());
        // A blank or absent key never wins, even if the predicate would say yes.
        assertEquals("Iron", MaterialNames.display(Identifier.parse("c:iron"), "  ", key -> true).getString());
        assertEquals("Iron", MaterialNames.display(Identifier.parse("c:iron"), null, key -> true).getString());
    }

    @Test
    void aTranslatedMaterialKeepsItsKeySoTheClientLanguageStillDecides() {
        Component name = MaterialNames.display(Identifier.parse("test:star"), "material.test.star",
                TRANSLATIONS::containsKey);
        assertInstanceOf(TranslatableContents.class, name.getContents());
        assertEquals("material.test.star", ((TranslatableContents) name.getContents()).getKey());
    }

    /** Resolves through the real production rule, then renders it against this test's tiny language. */
    private String name(Entry entry) {
        Component resolved = MaterialNames.display(entry.id(), entry.displayKey(), TRANSLATIONS::containsKey);
        return resolved.getContents() instanceof TranslatableContents translatable
                ? TRANSLATIONS.get(translatable.getKey()) : resolved.getString();
    }

    private static List<Entry> catalogue() {
        return List.of(entry("test:echo", FragmentTier.VOIDITE),
                entry("test:iron", FragmentTier.FORGITE),
                entry("test:star", FragmentTier.COLONITE),
                entry("test:coal", FragmentTier.TERRITE),
                entry("test:diamond", FragmentTier.ORBITE),
                entry("test:aluminium", FragmentTier.FORGITE));
    }

    private static Entry entry(String id, FragmentTier tier) {
        return new Entry(Identifier.parse(id), tier, "material." + id.replace(':', '.'), 0x7FA0C0);
    }
}
