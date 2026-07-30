package za.co.neroland.neroagriculture.machine;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;

import za.co.neroland.neroagriculture.recipe.FabricationRecipe;

/**
 * Deterministic per-{@link RecipeType} index of the server's fabrication recipes, sorted by recipe id so
 * lookup order matches the previous full-stream sort exactly. The index is rebuilt only when the recipe
 * collection changes identity or size (i.e. on datapack reload) instead of streaming and sorting every
 * registered recipe on every machine tick.
 */
final class FabricationRecipeCache {
    private record Index(Object recipesIdentity, int size, Map<RecipeType<?>, List<FabricationRecipe>> byType) { }

    private static volatile Index index;

    private FabricationRecipeCache() { }

    /** Id-sorted fabrication recipes of {@code type}; an immutable shared list, never {@code null}. */
    static List<FabricationRecipe> recipes(ServerLevel level, RecipeType<FabricationRecipe> type) {
        Collection<RecipeHolder<?>> recipes = level.recipeAccess().getRecipes();
        Index current = index;
        if (current == null || current.recipesIdentity() != recipes || current.size() != recipes.size()) {
            current = rebuild(recipes);
            index = current;
        }
        return current.byType().getOrDefault(type, List.of());
    }

    private static Index rebuild(Collection<RecipeHolder<?>> recipes) {
        Map<RecipeType<?>, List<FabricationRecipe>> byType = recipes.stream()
                .sorted(Comparator.comparing(holder -> holder.id().identifier().toString()))
                .map(RecipeHolder::value)
                .filter(recipe -> recipe instanceof FabricationRecipe)
                .map(recipe -> (FabricationRecipe) recipe)
                .collect(Collectors.groupingBy(recipe -> (RecipeType<?>) recipe.getType(),
                        Collectors.collectingAndThen(Collectors.toList(), List::copyOf)));
        return new Index(recipes, recipes.size(), Map.copyOf(byType));
    }
}
