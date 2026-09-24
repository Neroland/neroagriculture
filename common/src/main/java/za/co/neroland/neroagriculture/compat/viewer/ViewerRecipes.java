package za.co.neroland.neroagriculture.compat.viewer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeMap;

import za.co.neroland.neroagriculture.recipe.FabricationRecipe;

/**
 * The client-side copy of NeroAgriculture's fabrication recipes, as synced by the server, for recipe viewers.
 *
 * <p>Since 26.x the vanilla client keeps no full recipe list, so a recipe viewer cannot read modded recipes
 * off the level. Both loaders opt the six fabrication serializers into the server's recipe sync (NeoForge
 * {@code OnDatapackSyncEvent.sendRecipes}, Fabric {@code RecipeSynchronization}) and hand what arrives to
 * {@link #accept}. Forge has no recipe viewer runtime on 26.x, so it has no sync.
 *
 * <p>Deliberately free of any recipe-viewer import, so the JEI and EMI plugins can both read it without one
 * dragging the other in. Nothing here is personal data — these are recipe definitions from the server's
 * datapacks.
 */
public final class ViewerRecipes {

    /** Written from the network thread, read from the render/reload thread — hence volatile. */
    private static volatile List<RecipeHolder<FabricationRecipe>> recipes = List.of();

    private ViewerRecipes() {
    }

    /** NeoForge hands over a {@link RecipeMap}. */
    public static void accept(RecipeMap recipeMap) {
        accept(recipeMap == null ? null : recipeMap.values());
    }

    /** Keeps only NeroAgriculture's fabrication recipes from whatever the server synced. */
    public static void accept(Collection<RecipeHolder<?>> holders) {
        recipes = holders == null ? List.of() : fabrication(holders);
    }

    /** Forgets the previous server's recipes on disconnect. */
    public static void clear() {
        recipes = List.of();
    }

    /** Every synced fabrication recipe; empty before the first sync or on a server that sends none. */
    public static List<RecipeHolder<FabricationRecipe>> all() {
        return recipes;
    }

    /** The fabrication recipes in any synced collection (EMI hands plugins its own copy). */
    public static List<RecipeHolder<FabricationRecipe>> fabrication(Collection<RecipeHolder<?>> holders) {
        List<RecipeHolder<FabricationRecipe>> out = new ArrayList<>();
        for (RecipeHolder<?> holder : holders) {
            if (holder.value() instanceof FabricationRecipe) {
                @SuppressWarnings("unchecked")
                RecipeHolder<FabricationRecipe> typed = (RecipeHolder<FabricationRecipe>) holder;
                out.add(typed);
            }
        }
        return List.copyOf(out);
    }
}
