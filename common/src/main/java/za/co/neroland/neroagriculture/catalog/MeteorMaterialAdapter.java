package za.co.neroland.neroagriculture.catalog;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import za.co.neroland.neroagriculture.catalog.CatalogResolver.Candidate;
import za.co.neroland.neroagriculture.catalog.MaterialDefinition.InputSelector;
import za.co.neroland.neroagriculture.catalog.MaterialDefinition.InputSelector.Kind;
import za.co.neroland.neroagriculture.catalog.MaterialDefinition.WorldRestriction;
import za.co.neroland.neroagriculture.catalog.MaterialDefinition.Yield;
import za.co.neroland.neroagriculture.content.FragmentTier;
import za.co.neroland.nerolandcore.meteor.MeteorMaterialEntry;

/** Read-only adaptation of Core metadata; Core's registry is never mutated. */
public final class MeteorMaterialAdapter {
    private MeteorMaterialAdapter() { }

    public static List<Candidate> adapt(Collection<MeteorMaterialEntry> entries) {
        List<Candidate> result = new ArrayList<>();
        for (MeteorMaterialEntry entry : entries) {
            FragmentTier tier = switch (entry.tier()) {
                case COMMON -> FragmentTier.FORGITE;
                case UNCOMMON -> FragmentTier.ORBITE;
                case RARE -> FragmentTier.COLONITE;
                case EXOTIC -> FragmentTier.VOIDITE;
            };
            MaterialDefinition definition = new MaterialDefinition(entry.id(), new InputSelector(Kind.ITEM, entry.item()),
                    entry.item(), tier, entry.minGate() == null ? MaterialDefinitionParser.defaultGate(tier) : entry.minGate(),
                    new Yield(1, tier.ordinal() + 3, 32 * (tier.ordinal() + 1)), 8 + tier.ordinal() * 4,
                    "material." + entry.id().getNamespace() + "." + entry.id().getPath().replace('/', '.'),
                    palette(entry.id().toString()), entry.enabled(), entry.planet() == null ? null : new WorldRestriction(entry.planet()));
            result.add(new Candidate(definition, CatalogSource.METEOR, "Core meteor metadata " + entry.id()));
        }
        return List.copyOf(result);
    }

    private static int palette(String value) {
        return 0x303030 | (value.hashCode() & 0xCFCFCF);
    }
}
