package za.co.neroland.neroagriculture.machine;

import java.util.Comparator;
import java.util.Optional;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import za.co.neroland.neroagriculture.catalog.MaterialDefinition;
import za.co.neroland.neroagriculture.catalog.ResolvedCatalog;
import za.co.neroland.neroagriculture.catalog.ResolvedMaterial;
import za.co.neroland.neroagriculture.content.EssenceFamily;
import za.co.neroland.neroagriculture.registry.ModItems;

/** Deterministic catalog matching and finite essence-family item mapping. */
public final class MaterialOperations {
    private MaterialOperations() { }

    public static Optional<ResolvedMaterial> match(ItemStack stack, ResolvedCatalog catalog) {
        return matchAny(stack, catalog).filter(value -> catalog.lookup(value.definition().id()).permitsGrowth());
    }

    public static Optional<ResolvedMaterial> matchAny(ItemStack stack, ResolvedCatalog catalog) {
        if (stack.isEmpty()) return Optional.empty();
        return catalog.all().values().stream()
                .sorted(Comparator.comparing(value -> value.definition().id().toString()))
                .filter(value -> matches(stack, value.definition().input()))
                .findFirst();
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

    public static Item neutralEssence(EssenceFamily family) {
        return switch (family) {
            case TERRAN -> ModItems.TERRAN_ESSENCE.get();
            case INDUSTRIAL -> ModItems.INDUSTRIAL_ESSENCE.get();
            case ORBITAL -> ModItems.ORBITAL_ESSENCE.get();
            case COLONIAL -> ModItems.COLONIAL_ESSENCE.get();
            case DEEPVOID -> ModItems.DEEPVOID_ESSENCE.get();
        };
    }
}
