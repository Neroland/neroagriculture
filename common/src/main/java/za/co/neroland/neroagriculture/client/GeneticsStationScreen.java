package za.co.neroland.neroagriculture.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import za.co.neroland.neroagriculture.genetics.Genetics;
import za.co.neroland.neroagriculture.menu.GeneticsStationMenu;
import za.co.neroland.neroagriculture.registry.ModDataComponents;

/**
 * Texture-free screen for the Genetics Station (drawn entirely with {@code fill}/{@code text}, matching
 * {@link FoundationMachineScreen}). Shows the two input slots, the locked output, an energy gauge and a
 * splice-progress bar, plus a live readout of the input seed's traits read straight off the synced slot
 * stack — so the station's genetics maths is visible instead of printed to chat.
 */
public final class GeneticsStationScreen extends AbstractContainerScreen<GeneticsStationMenu> {
    private static final int HULL = 0xFFE6ECF0;
    private static final int HULL_HI = 0xFFF3F7FA;
    private static final int EDGE = 0xFF59656F;
    private static final int DIVIDER = 0xFFB4C0C8;
    private static final int WELL = 0xFF9AA6AE;
    private static final int WELL_EDGE = 0xFF3A444C;
    private static final int ACCENT = 0xFF78C860;
    private static final int TEXT = 0xFF1E2C34;
    private static final int MUTED = 0xFF5E707C;

    public GeneticsStationScreen(GeneticsStationMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 166);
        titleLabelX = 8;
        inventoryLabelX = 8;
    }

    @Override public void extractContents(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        int x = leftPos;
        int y = topPos;
        int w = imageWidth;
        int h = imageHeight;

        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, EDGE);
        g.fill(x, y, x + w, y + h, HULL);
        g.fill(x, y, x + w, y + 17, HULL_HI);
        g.fill(x + 7, y + 15, x + w - 7, y + 16, ACCENT);
        g.fill(x + 7, y + 16, x + w - 7, y + 17, DIVIDER);
        g.fill(x + 7, y + 71, x + w - 7, y + 72, DIVIDER);

        for (Slot slot : menu.slots) {
            if (slot.x < 0 || slot.y < 0) continue;
            int sx = x + slot.x;
            int sy = y + slot.y;
            g.fill(sx - 1, sy - 1, sx + 17, sy + 17, WELL_EDGE);
            g.fill(sx, sy, sx + 16, sy + 16, WELL);
        }

        // splice arrow from inputs toward the output
        int ay = y + 40;
        g.fill(x + 86, ay, x + 114, ay + 1, DIVIDER);
        g.fill(x + 112, ay - 2, x + 114, ay + 3, ACCENT);

        // labelled energy gauge above the slot row
        Gauges.bar(g, font, x + 8, y + 18, x + 168, "Energy", menu.energy(), 100_000, Gauges.ENERGY);

        // labelled splice-progress bar under the trait readout
        Gauges.bar(g, font, x + 8, y + 60, x + 168, "Splicing", menu.progress(),
                Math.max(1, menu.maxProgress()), Gauges.PROGRESS);

        // live trait readout for the input seed (slot 0), between the slots and the progress bar
        ItemStack input = menu.slots.get(0).getItem();
        Genetics genetics = input.get(ModDataComponents.GENETICS.get());
        Component traits = genetics == null
                ? Component.translatable("screen.neroagriculture.genetics.insert_seed")
                : Component.literal("Y" + genetics.yield() + " S" + genetics.speed() + " H" + genetics.hardiness()
                        + " O" + genetics.oxygenOutput() + " P" + genetics.foodPotency()
                        + "  ∑" + genetics.total() + "/15");
        g.text(font, traits, x + 8, y + 50, MUTED, false);

        super.extractContents(g, mouseX, mouseY, partialTick);
    }

    @Override protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        g.text(font, title, titleLabelX, titleLabelY, TEXT, false);
        g.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, MUTED, false);
    }
}
