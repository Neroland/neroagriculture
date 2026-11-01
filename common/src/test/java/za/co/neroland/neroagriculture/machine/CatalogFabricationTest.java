package za.co.neroland.neroagriculture.machine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;
import za.co.neroland.neroagriculture.catalog.CatalogSource;
import za.co.neroland.neroagriculture.catalog.CatalogResolver;
import za.co.neroland.neroagriculture.catalog.MaterialDefinition;
import za.co.neroland.neroagriculture.content.FragmentTier;

class CatalogFabricationTest {
    @Test void fallbackRequiresAnEligibleSourceAndNeverOverridesAnExplicitRecipe() {
        assertTrue(CatalogFabrication.eligible(CatalogSource.ORE_TAG, Identifier.parse("c:tin"), false));
        assertTrue(CatalogFabrication.eligible(CatalogSource.DATAPACK, Identifier.parse("example:crystal"), false));
        assertFalse(CatalogFabrication.eligible(CatalogSource.ORE_TAG, Identifier.parse("c:unknown_relic"), false));
        assertFalse(CatalogFabrication.eligible(CatalogSource.ORE_TAG, Identifier.parse("c:tin"), true));
        assertFalse(CatalogFabrication.eligible(CatalogSource.ORE_TAG, Identifier.parse("other:tin"), false));
        assertFalse(CatalogFabrication.eligible(CatalogSource.METEOR, Identifier.parse("nerospace:alien_core"), false));
    }

    @Test void fallbackCostsRiseByTier() {
        assertEquals(800, CatalogFabrication.extractionEnergy(FragmentTier.TERRITE));
        assertEquals(4000, CatalogFabrication.extractionEnergy(FragmentTier.VOIDITE));
        for (int i = 1; i < FragmentTier.values().length; i++) {
            assertTrue(CatalogFabrication.extractionEnergy(FragmentTier.values()[i])
                    > CatalogFabrication.extractionEnergy(FragmentTier.values()[i - 1]));
        }
    }

    @Test void explicitMaterialIsMatchedBeforeACompetingGenericTag() {
        var input = new MaterialDefinition.InputSelector(MaterialDefinition.InputSelector.Kind.ITEM,
                Identifier.parse("example:sample"));
        var generic = definition("c:sample", input);
        var curated = definition("example:sample", input);
        var catalog = CatalogResolver.resolve(java.util.List.of(
                new CatalogResolver.Candidate(generic, CatalogSource.ORE_TAG, "tag"),
                new CatalogResolver.Candidate(curated, CatalogSource.DATAPACK, "pack")),
                java.util.Set.of(), java.util.Map.of(), 16, java.util.List.of());
        assertEquals(curated.id(), MaterialOperations.sortedMaterials(catalog).getFirst().definition().id());
    }

    private static MaterialDefinition definition(String id, MaterialDefinition.InputSelector input) {
        return new MaterialDefinition(Identifier.parse(id), input, Identifier.parse("example:sample"),
                FragmentTier.ORBITE, null, new MaterialDefinition.Yield(1, 3, 32), 16,
                "material.example.sample", 0x777777, true, null, 0);
    }
}
