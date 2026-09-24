package za.co.neroland.neroagriculture.compat.emi;

import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiStack;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Block;

import za.co.neroland.neroagriculture.NeroAgricultureCommon;
import za.co.neroland.neroagriculture.compat.viewer.FabricationDisplay;
import za.co.neroland.neroagriculture.compat.viewer.FabricationPage;
import za.co.neroland.neroagriculture.compat.viewer.ViewerRecipes;
import za.co.neroland.neroagriculture.recipe.FabricationRecipe;

/**
 * NeroAgriculture's EMI integration — the native counterpart of {@code compat.jei.NeroAgricultureJeiPlugin},
 * with the same six fabrication pages and workstations.
 *
 * <p>Built against the community "EMI Unofficial Port" (official EMI has no Minecraft 26.x release), which
 * keeps the upstream {@code dev.emi.emi.api} package. Only that API is used, so the class lives in
 * {@code common}: NeoForge finds it through {@link EmiEntrypoint}, Fabric through the {@code emi} entrypoint in
 * {@code fabric.mod.json}. EMI is compile-time only; without it this class is never loaded. Nothing here may
 * touch a {@code compat.jei} class — those extend JEI types and would not load in an EMI-only install. With
 * this plugin present, EMI's JEI bridge skips the {@code neroagriculture} JEI pages, so JEI + EMI together do
 * not show them twice.
 *
 * <p>Recipes: EMI syncs every recipe type itself and hands the result to plugins, reloading only after it
 * arrives, so that copy is read first. {@link ViewerRecipes} (filled by NeroAgriculture's own loader wiring)
 * is the fallback.
 */
@EmiEntrypoint
public final class NeroAgricultureEmiPlugin implements EmiPlugin {

    private static final Map<FabricationPage, EmiRecipeCategory> CATEGORIES = new EnumMap<>(FabricationPage.class);

    static {
        for (FabricationPage page : FabricationPage.values()) {
            CATEGORIES.put(page, new EmiRecipeCategory(
                    Identifier.fromNamespaceAndPath(NeroAgricultureCommon.MOD_ID, page.path()),
                    EmiStack.of(page.workstations().get(0))));
        }
    }

    @Override
    public void register(EmiRegistry registry) {
        for (FabricationPage page : FabricationPage.values()) {
            EmiRecipeCategory category = CATEGORIES.get(page);
            registry.addCategory(category);
            for (Block workstation : page.workstations()) {
                registry.addWorkstation(category, EmiStack.of(workstation));
            }
        }
        for (FabricationDisplay display : FabricationDisplay.all(syncedRecipes(registry))) {
            registry.addRecipe(new FabricationEmiRecipe(CATEGORIES.get(display.page()), display));
        }
    }

    /** EMI's copy of the synced fabrication recipes, else NeroAgriculture's own copy of the same packet. */
    private static List<RecipeHolder<FabricationRecipe>> syncedRecipes(EmiRegistry registry) {
        //? if >=26.3 {
        /*Collection<RecipeHolder<?>> fromEmi = registry.getRecipes();
        *///?} else {
        net.minecraft.world.item.crafting.RecipeMap recipeMap = registry.getRecipeMap();
        Collection<RecipeHolder<?>> fromEmi = recipeMap == null ? null : recipeMap.values();
        //?}
        List<RecipeHolder<FabricationRecipe>> fabrication = fromEmi == null ? List.of() : ViewerRecipes.fabrication(fromEmi);
        return fabrication.isEmpty() ? ViewerRecipes.all() : fabrication;
    }
}
