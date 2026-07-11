package za.co.neroland.neroagriculture.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.resources.Identifier;

import org.junit.jupiter.api.Test;

import za.co.neroland.neroagriculture.catalog.CatalogResolver.Candidate;
import za.co.neroland.neroagriculture.content.EssenceFamily;
import za.co.neroland.neroagriculture.crop.CropVariantState;

class CatalogReloadSafetyTest {
    @Test
    void addChangeRemoveAndReaddNeverReassignStoredId() {
        Identifier id = Identifier.parse("pack:osmium");
        CropVariantState stored = CropVariantState.fresh(id).harvested().harvested();
        ResolvedCatalog added = catalog(definition(id, EssenceFamily.ORBITAL, 0x778899));
        assertTrue(added.lookup(stored.material()).permitsGrowth());

        ResolvedCatalog changed = catalog(definition(id, EssenceFamily.COLONIAL, 0x998877));
        assertEquals(id, stored.material());
        assertEquals(2, stored.harvestCount());
        assertEquals(EssenceFamily.COLONIAL, changed.lookup(id).material().orElseThrow().definition().tier());

        ResolvedCatalog removed = CatalogResolver.resolve(List.of(), Set.of(), Map.of(), 512, List.of());
        assertFalse(removed.lookup(stored.material()).permitsGrowth());
        assertEquals(ResolvedCatalog.Status.UNKNOWN, removed.lookup(stored.material()).status());

        ResolvedCatalog readded = catalog(definition(id, EssenceFamily.DEEPVOID, 0x554466));
        assertTrue(readded.lookup(stored.material()).permitsGrowth());
        assertEquals(id, stored.material());
    }

    private static ResolvedCatalog catalog(MaterialDefinition definition) {
        return CatalogResolver.resolve(List.of(new Candidate(definition, CatalogSource.DATAPACK, "test pack")),
                Set.of(), Map.of(), 512, List.of());
    }

    private static MaterialDefinition definition(Identifier id, EssenceFamily tier, int color) {
        return new MaterialDefinition(id, new MaterialDefinition.InputSelector(
                MaterialDefinition.InputSelector.Kind.ITEM, Identifier.parse("minecraft:iron_ingot")),
                Identifier.parse("minecraft:iron_ingot"), tier, MaterialDefinitionParser.defaultGate(tier),
                new MaterialDefinition.Yield(1, 4, 32), 8, "material.pack.osmium", color, true, null);
    }
}
