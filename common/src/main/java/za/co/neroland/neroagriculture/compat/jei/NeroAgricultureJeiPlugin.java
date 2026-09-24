package za.co.neroland.neroagriculture.compat.jei;

import java.util.List;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;

import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;

import za.co.neroland.neroagriculture.NeroAgricultureCommon;
import za.co.neroland.neroagriculture.compat.viewer.FabricationDisplay;
import za.co.neroland.neroagriculture.compat.viewer.FabricationPage;
import za.co.neroland.neroagriculture.compat.viewer.ViewerRecipes;

/**
 * NeroAgriculture's Just Enough Items integration: one page per fabrication recipe type (extraction,
 * infusing, fusion, synthesizing, conversion, research), with the machines that run them as crafting
 * stations. Crafting recipes need no code; JEI reads those itself.
 *
 * <p>Uses only the loader-agnostic JEI common API, so the class lives in {@code common} and JEI finds it
 * through {@link JeiPlugin} on every loader. JEI is compile-time only; without it this class never loads.
 * Recipes come from {@link ViewerRecipes}, which each loader fills from the server's recipe sync.
 */
@JeiPlugin
public final class NeroAgricultureJeiPlugin implements IModPlugin {

    private static final Identifier UID = Identifier.fromNamespaceAndPath(NeroAgricultureCommon.MOD_ID, "jei");

    @Override
    public Identifier getPluginUid() {
        return UID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        IGuiHelper guiHelper = registration.getJeiHelpers().getGuiHelper();
        for (FabricationPage page : FabricationPage.values()) {
            registration.addRecipeCategories(new FabricationCategory(guiHelper, page));
        }
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        List<FabricationDisplay> displays = FabricationDisplay.all(ViewerRecipes.all());
        for (FabricationPage page : FabricationPage.values()) {
            registration.addRecipes(FabricationCategory.type(page),
                    displays.stream().filter(display -> display.page() == page).toList());
        }
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        for (FabricationPage page : FabricationPage.values()) {
            registration.addCraftingStation(FabricationCategory.type(page),
                    page.workstations().toArray(Block[]::new));
        }
    }
}
