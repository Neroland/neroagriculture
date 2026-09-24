package za.co.neroland.neroagriculture.compat.jei;

import java.util.List;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.placement.HorizontalAlignment;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeType;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.world.item.ItemStack;

import za.co.neroland.neroagriculture.NeroAgricultureCommon;
import za.co.neroland.neroagriculture.compat.viewer.FabricationDisplay;
import za.co.neroland.neroagriculture.compat.viewer.FabricationPage;

/**
 * One JEI page for one fabrication recipe type: the input slots, an arrow, the output slots, and the energy
 * and time underneath. The layout comes from {@link FabricationDisplay}, which the EMI page shares.
 */
public final class FabricationCategory extends AbstractRecipeCategory<FabricationDisplay> {

    static final int WIDTH = 142;
    static final int HEIGHT = 46;
    private static final int SLOT = 20;
    private static final int TOP = 5;
    /** Output slots draw a 26px background, so they need wider spacing than inputs. */
    private static final int OUTPUT_SLOT = 26;
    private static final int TEXT_Y = 28;

    public FabricationCategory(IGuiHelper guiHelper, FabricationPage page) {
        super(type(page), Component.translatable(page.titleKey()),
                guiHelper.createDrawableItemLike(page.workstations().get(0)), WIDTH, HEIGHT);
    }

    /** The JEI recipe type for a page, keyed by the same id as the vanilla recipe type. */
    public static IRecipeType<FabricationDisplay> type(FabricationPage page) {
        return IRecipeType.create(NeroAgricultureCommon.MOD_ID, page.path(), FabricationDisplay.class);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, FabricationDisplay display, IFocusGroup focuses) {
        int x = 2;
        for (List<ItemStack> input : display.inputs()) {
            builder.addInputSlot(x, TOP).setStandardSlotBackground().addItemStacks(input);
            x += SLOT;
        }
        x = arrowX(display) + 30;
        for (ItemStack output : display.outputs()) {
            if (display.unlockOnly()) {
                // Researching a resource unlocks its seed rather than handing one over, so the seed is shown
                // but not registered as an output JEI would offer as a way to make it.
                builder.addSlot(RecipeIngredientRole.RENDER_ONLY, x, TOP).setStandardSlotBackground().add(output);
            } else {
                builder.addOutputSlot(x, TOP).setOutputSlotBackground().add(output);
            }
            x += OUTPUT_SLOT;
        }
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, FabricationDisplay display, IFocusGroup focuses) {
        builder.addRecipeArrowWidget().setPosition(arrowX(display), TOP + 1);
        builder.addText(List.<FormattedText>copyOf(display.statLines()), WIDTH, HEIGHT - TEXT_Y)
                .setPosition(0, TEXT_Y)
                .setTextAlignment(HorizontalAlignment.CENTER)
                .setColor(0xFF808080);
    }

    private static int arrowX(FabricationDisplay display) {
        return 2 + display.inputs().size() * SLOT + 2;
    }
}
