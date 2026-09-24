package za.co.neroland.neroagriculture.compat.emi;

import java.util.List;

import dev.emi.emi.api.recipe.BasicEmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.render.EmiTexture;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.TextWidget;
import dev.emi.emi.api.widget.WidgetHolder;

import net.minecraft.network.chat.Component;

import za.co.neroland.neroagriculture.compat.viewer.FabricationDisplay;

/**
 * One fabrication recipe on its EMI page — the twin of {@code compat.jei.FabricationCategory}, laid out from
 * the same {@link FabricationDisplay}: input slots, an arrow, output slots, then energy and time.
 */
final class FabricationEmiRecipe extends BasicEmiRecipe {

    private static final int WIDTH = 142;
    private static final int HEIGHT = 46;
    private static final int SLOT = 20;
    private static final int TOP = 4;
    private static final int TEXT_Y = 28;
    private static final int LINE_HEIGHT = 10;
    /** Same grey as the JEI page. */
    private static final int STAT_COLOR = 0xFF808080;

    private final FabricationDisplay display;

    FabricationEmiRecipe(EmiRecipeCategory category, FabricationDisplay display) {
        super(category, display.id(), WIDTH, HEIGHT);
        this.display = display;
        this.inputs = display.inputs().stream()
                .map(stacks -> EmiIngredient.of(stacks.stream().map(EmiStack::of).toList()))
                .toList();
        // Researching a resource unlocks its seed rather than handing one over; list it as a catalyst-style
        // display only, so EMI never offers it as a way to make the seed.
        this.outputs = display.unlockOnly() ? List.of() : display.outputs().stream().map(EmiStack::of).toList();
    }

    @Override
    public boolean supportsRecipeTree() {
        return !display.unlockOnly();
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        int x = 2;
        for (EmiIngredient input : inputs) {
            widgets.addSlot(input, x, TOP);
            x += SLOT;
        }
        int arrowX = x + 2;
        widgets.addTexture(EmiTexture.EMPTY_ARROW, arrowX, TOP + 1);
        x = arrowX + 30;
        for (var output : display.outputs()) {
            var slot = widgets.addSlot(EmiStack.of(output), x, TOP);
            if (!display.unlockOnly()) {
                slot.recipeContext(this);
            }
            x += SLOT;
        }
        List<Component> lines = display.statLines();
        for (int i = 0; i < lines.size(); i++) {
            widgets.addText(lines.get(i), WIDTH / 2, TEXT_Y + i * LINE_HEIGHT, STAT_COLOR, false)
                    .horizontalAlign(TextWidget.Alignment.CENTER);
        }
    }
}
