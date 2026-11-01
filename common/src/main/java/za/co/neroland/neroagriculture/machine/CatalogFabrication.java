package za.co.neroland.neroagriculture.machine;

import java.util.Set;

import net.minecraft.resources.Identifier;
import za.co.neroland.neroagriculture.catalog.CatalogSource;
import za.co.neroland.neroagriculture.content.FragmentTier;

/** Conservative fallback for explicitly defined materials and common tagged metals. */
public final class CatalogFabrication {
    private static final Set<String> COMMON_METALS = Set.of("tin", "lead", "silver", "nickel", "zinc",
            "aluminum", "aluminium");

    private CatalogFabrication() { }

    public static boolean eligible(CatalogSource source, Identifier id, boolean explicitRecipeExists) {
        if (explicitRecipeExists) return false;
        return switch (source) {
            case DATAPACK, CONFIG -> true;
            case ORE_TAG -> id.getNamespace().equals("c") && COMMON_METALS.contains(id.getPath());
            default -> false;
        };
    }

    public static int extractionEnergy(FragmentTier tier) {
        return new int[] {800, 1200, 1800, 2800, 4000}[tier.ordinal()];
    }

    public static int extractionTicks(FragmentTier tier) {
        return new int[] {80, 100, 120, 160, 200}[tier.ordinal()];
    }
}
