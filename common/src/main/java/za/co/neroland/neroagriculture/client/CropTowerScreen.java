package za.co.neroland.neroagriculture.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

import za.co.neroland.neroagriculture.menu.CropTowerMenu;

/**
 * Texture-free screen for the Crop Tower Controller (drawn with {@code fill}/{@code text} like
 * {@link FoundationMachineScreen}): three seed slots plus a fertiliser slot on the left, six output
 * slots on the right, an energy gauge, a nutrient gauge, and a readout of tower height and active slots.
 */
public final class CropTowerScreen extends AbstractContainerScreen<CropTowerMenu> {
    private static final int HULL = 0xFFE6ECF0;
    private static final int HULL_HI = 0xFFF3F7FA;
    private static final int EDGE = 0xFF59656F;
    private static final int DIVIDER = 0xFFB4C0C8;
    private static final int WELL = 0xFF9AA6AE;
    private static final int WELL_EDGE = 0xFF3A444C;
    private static final int TROUGH = 0xFF2A343C;
    private static final int ENERGY = 0xFFE0B33A;
    private static final int NUTRIENT = 0xFF42A880;
    private static final int ACCENT = 0xFF78C860;
    private static final int TEXT = 0xFF1E2C34;
    private static final int MUTED = 0xFF5E707C;

    public CropTowerScreen(CropTowerMenu menu, Inventory inventory, Component title) {
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

        // feed arrow toward the output cluster
        g.fill(x + 86, y + 27, x + 114, y + 28, DIVIDER);
        g.fill(x + 112, y + 25, x + 114, y + 30, ACCENT);

        // energy + nutrient gauges
        int energyFrac = Math.min(96, menu.energy() * 96 / 100_000);
        g.fill(x + 8, y + 60, x + 104, y + 63, TROUGH);
        if (energyFrac > 0) g.fill(x + 8, y + 60, x + 8 + energyFrac, y + 63, ENERGY);
        int nutrientFrac = Math.min(96, menu.nutrient() * 96 / 8_000);
        g.fill(x + 8, y + 65, x + 104, y + 68, TROUGH);
        if (nutrientFrac > 0) g.fill(x + 8, y + 65, x + 8 + nutrientFrac, y + 68, NUTRIENT);

        Component tower = menu.height() <= 0
                ? Component.translatable("screen.neroagriculture.tower.unformed")
                : Component.translatable("screen.neroagriculture.tower.status", menu.height(), menu.activeSlots());
        g.text(font, tower, x + 8, y + 50, MUTED, false);

        super.extractContents(g, mouseX, mouseY, partialTick);
    }

    @Override protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        g.text(font, title, titleLabelX, titleLabelY, TEXT, false);
        g.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, MUTED, false);
    }
}
