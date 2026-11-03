package za.co.neroland.neroagriculture.machine;

import java.util.Set;

import net.minecraft.resources.Identifier;
import za.co.neroland.neroagriculture.catalog.CatalogSource;
import za.co.neroland.neroagriculture.catalog.MaterialDefinition;
import za.co.neroland.neroagriculture.content.FragmentTier;

/** Conservative fallback for explicitly defined materials and common tagged metals. */
public final class CatalogFabrication {
    private static final Set<String> COMMON_METALS = Set.of("tin", "lead", "silver", "nickel", "zinc",
            "aluminum", "aluminium");

    private CatalogFabrication() { }

    public static boolean eligible(CatalogSource source, MaterialDefinition definition, boolean explicitRecipeExists) {
        if (explicitRecipeExists) return false;
        Identifier id = definition.id();
        var selector = definition.input();
        return switch (source) {
            case DATAPACK -> true;
            case CONFIG -> selector.kind() == MaterialDefinition.InputSelector.Kind.ITEM
                    || safeDiscoveredMetal(id, selector);
            case ORE_TAG -> safeDiscoveredMetal(id, selector);
            default -> false;
        };
    }

    private static boolean safeDiscoveredMetal(Identifier id, MaterialDefinition.InputSelector selector) {
        if (!id.getNamespace().equals("c") || !COMMON_METALS.contains(id.getPath())
                || selector.kind() != MaterialDefinition.InputSelector.Kind.TAG
                || !selector.id().getNamespace().equals("c")) return false;
        String path = selector.id().getPath();
        return path.equals("ingots/" + id.getPath()) || path.equals("raw_materials/" + id.getPath())
                || path.equals("dusts/" + id.getPath());
    }

    public static boolean genericConversionAllowed(CatalogSource source, MaterialDefinition definition) {
        var input = definition.input();
        return source == CatalogSource.DATAPACK
                || input.kind() != MaterialDefinition.InputSelector.Kind.TAG
                || !input.id().getNamespace().equals("c")
                || !input.id().getPath().startsWith("ores/");
    }

    public static int extractionEnergy(FragmentTier tier) {
        return new int[] {800, 1200, 1800, 2800, 4000}[tier.ordinal()];
    }

    public static int extractionTicks(FragmentTier tier) {
        return new int[] {80, 100, 120, 160, 200}[tier.ordinal()];
    }
}
