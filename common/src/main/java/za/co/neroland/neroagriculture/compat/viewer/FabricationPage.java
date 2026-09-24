package za.co.neroland.neroagriculture.compat.viewer;

import java.util.List;
import java.util.function.Supplier;

import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;

import za.co.neroland.neroagriculture.recipe.FabricationRecipe;
import za.co.neroland.neroagriculture.registry.ModBlocks;
import za.co.neroland.neroagriculture.registry.ModRecipeSerializers;

/**
 * The six recipe-viewer pages, one per fabrication recipe type, with the machines that run each. Shared by
 * the JEI and EMI plugins so both viewers show the same pages. The page id matches the recipe type path.
 */
public enum FabricationPage {
    MATERIAL_EXTRACTION("material_extraction", ModRecipeSerializers.EXTRACTION::get,
            List.of(ModBlocks.FRAGMENT_EXTRACTOR::get)),
    FRAGMENT_INFUSING("fragment_infusing", ModRecipeSerializers.INFUSING::get,
            List.of(ModBlocks.FRAGMENT_INFUSER::get)),
    FRAGMENT_FUSION("fragment_fusion", ModRecipeSerializers.FUSION::get,
            List.of(ModBlocks.FRAGMENT_INFUSER::get)),
    SEED_SYNTHESIZING("seed_synthesizing", ModRecipeSerializers.SYNTHESIZING::get,
            List.of(ModBlocks.SEED_SYNTHESIZER::get)),
    MATERIAL_CONVERSION("material_conversion", ModRecipeSerializers.CONVERSION::get,
            List.of(ModBlocks.SEED_SYNTHESIZER::get)),
    SEED_RESEARCHING("seed_researching", ModRecipeSerializers.RESEARCHING::get,
            List.of(ModBlocks.SEED_RESEARCH_BENCH::get));

    private final String path;
    private final Supplier<RecipeType<FabricationRecipe>> type;
    private final List<Supplier<? extends Block>> workstations;

    FabricationPage(String path, Supplier<RecipeType<FabricationRecipe>> type,
            List<Supplier<? extends Block>> workstations) {
        this.path = path;
        this.type = type;
        this.workstations = workstations;
    }

    /** The page id path, identical to the recipe type's path. */
    public String path() {
        return path;
    }

    /** Translation key for the page title. */
    public String titleKey() {
        return "gui.neroagriculture.viewer." + path;
    }

    public RecipeType<FabricationRecipe> type() {
        return type.get();
    }

    /** The machines that run this page's recipes; the first is the page icon. */
    public List<Block> workstations() {
        return workstations.stream().<Block>map(Supplier::get).toList();
    }

    /** The page for a recipe's type, or {@code null} for a type this enum does not know. */
    public static FabricationPage of(RecipeType<?> type) {
        for (FabricationPage page : values()) {
            if (page.type() == type) {
                return page;
            }
        }
        return null;
    }
}
