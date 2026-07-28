package za.co.neroland.neroagriculture.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

import za.co.neroland.neroagriculture.menu.ProcessorMenu;

/**
 * Texture-free screen shared by the Bioreactor, Biofuel Converter and Fertiliser Processor —
 * input slot(s) → progress arrow → output, with an energy gauge.
 */
public final class ProcessorScreen extends AbstractContainerScreen<ProcessorMenu> {
    private static final int HULL = 0xFFE6ECF0;
    private static final int HULL_HI = 0xFFF3F7FA;
    private static final int EDGE = 0xFF59656F;
    private static final int DIVIDER = 0xFFB4C0C8;
    private static final int WELL = 0xFF9AA6AE;
    private static final int WELL_EDGE = 0xFF3A444C;
    private static final int TROUGH = 0xFF2A343C;
    private static final int PROGRESS = 0xFF78C860;
    private static final int ACCENT = 0xFF78C860;
    private static final int TEXT = 0xFF1E2C34;
    private static final int MUTED = 0xFF5E707C;

    public ProcessorScreen(ProcessorMenu menu, Inventory inventory, Component title) {
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

        // labelled energy gauge (synced as a fraction of the live capacity — see menu.GaugeData)
        Gauges.bar(g, font, x + 8, y + 18, x + 168, "Energy", menu.energy(),
                za.co.neroland.neroagriculture.menu.GaugeData.SCALE, Gauges.ENERGY);

        // progress arrow between inputs and output
        g.fill(x + 88, y + 37, x + 112, y + 39, TROUGH);
        int progressWidth = menu.maxProgress() <= 0 ? 0 : Math.min(24, menu.progress() * 24 / menu.maxProgress());
        if (progressWidth > 0) g.fill(x + 88, y + 37, x + 88 + progressWidth, y + 39, PROGRESS);

        // labelled work bar under the machine row
        Gauges.bar(g, font, x + 8, y + 56, x + 168, "Progress", menu.progress(),
                Math.max(1, menu.maxProgress()), Gauges.PROGRESS);

        super.extractContents(g, mouseX, mouseY, partialTick);
    }

    @Override protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        g.text(font, title, titleLabelX, titleLabelY, TEXT, false);
        g.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, MUTED, false);
    }
}
