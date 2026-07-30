package za.co.neroland.neroagriculture.machine;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import za.co.neroland.neroagriculture.catalog.MaterialDefinition;
import za.co.neroland.neroagriculture.catalog.ResolvedCatalog;
import za.co.neroland.neroagriculture.catalog.ResolvedMaterial;
import za.co.neroland.neroagriculture.content.FragmentTier;
import za.co.neroland.neroagriculture.registry.ModItems;

/** Deterministic catalog matching and finite fragment-family item mapping. */
public final class MaterialOperations {
    private MaterialOperations() { }

    public static Optional<ResolvedMaterial> match(ItemStack stack, ResolvedCatalog catalog) {
        return matchAny(stack, catalog).filter(value -> catalog.lookup(value.definition().id()).permitsGrowth());
    }

    public static Optional<ResolvedMaterial> matchAny(ItemStack stack, ResolvedCatalog catalog) {
        if (stack.isEmpty()) return Optional.empty();
        for (ResolvedMaterial value : sortedMaterials(catalog)) {
            if (matches(stack, value.definition().input())) return Optional.of(value);
        }
        return Optional.empty();
    }

    /** One-slot cache of the id-sorted material list, keyed on the immutable catalog snapshot's identity. */
    private record SortedCache(ResolvedCatalog catalog, List<ResolvedMaterial> sorted) { }

    private static volatile SortedCache sortedCache;

    private static List<ResolvedMaterial> sortedMaterials(ResolvedCatalog catalog) {
        SortedCache cache = sortedCache;
        if (cache == null || cache.catalog() != catalog) {
            cache = new SortedCache(catalog, catalog.all().values().stream()
                    .sorted(Comparator.comparing(value -> value.definition().id().toString())).toList());
            sortedCache = cache;
        }
        return cache.sorted();
    }

    public static boolean matches(ItemStack stack, MaterialDefinition.InputSelector selector) {
        return switch (selector.kind()) {
            case ITEM -> {
                Item item = BuiltInRegistries.ITEM.getValue(selector.id());
                yield item != null && stack.is(item);
            }
            case TAG -> stack.is(TagKey.create(Registries.ITEM, selector.id()));
        };
    }

    public static Item neutralFragment(FragmentTier family) {
        return switch (family) {
            case TERRITE -> ModItems.TERRITE_FRAGMENT.get();
            case FORGITE -> ModItems.FORGITE_FRAGMENT.get();
            case ORBITE -> ModItems.ORBITE_FRAGMENT.get();
            case COLONITE -> ModItems.COLONITE_FRAGMENT.get();
            case VOIDITE -> ModItems.VOIDITE_FRAGMENT.get();
        };
    }
}
