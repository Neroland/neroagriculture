package za.co.neroland.neroagriculture.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

import za.co.neroland.neroagriculture.menu.AreaMachineMenu;

/**
 * Texture-free screen for the Planter / Harvester / Fertiliser Applicator (all one block entity): a 3x3
 * seed/output grid, three upgrade slots, an energy gauge, and a readout of the current mode and working
 * range — replacing the chat status line.
 */
public final class AreaMachineScreen extends AbstractContainerScreen<AreaMachineMenu> {
    private static final int HULL = 0xFFE6ECF0;
    private static final int HULL_HI = 0xFFF3F7FA;
    private static final int EDGE = 0xFF59656F;
    private static final int DIVIDER = 0xFFB4C0C8;
    private static final int WELL = 0xFF9AA6AE;
    private static final int WELL_EDGE = 0xFF3A444C;
    private static final int TROUGH = 0xFF2A343C;
    private static final int ENERGY = 0xFFE0B33A;
    private static final int ACCENT = 0xFF78C860;
    private static final int TEXT = 0xFF1E2C34;
    private static final int MUTED = 0xFF5E707C;

    private static final String[] MODES = {"Planting", "Harvesting", "Applying"};

    @org.jetbrains.annotations.Nullable private net.minecraft.core.BlockPos machinePos;

    public AreaMachineScreen(AreaMachineMenu menu, Inventory inventory, Component title) {
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

        int energyFrac = Math.min(72, menu.energy() * 72 / 100_000);
        g.fill(x + 92, y + 60, x + 164, y + 63, TROUGH);
        if (energyFrac > 0) g.fill(x + 92, y + 60, x + 92 + energyFrac, y + 63, ENERGY);

        int mode = menu.mode();
        String modeName = mode >= 0 && mode < MODES.length ? MODES[mode] : "Idle";
        g.text(font, Component.literal(modeName + " — range " + menu.range() + "x" + menu.range()),
                x + 92, y + 22, MUTED, false);

        // "Show area" toggle pill — outlines the working radius in-world with hologram particles.
        int pillColor = menu.showArea() ? ACCENT : MUTED;
        g.fill(x + 92, y + 32, x + 164, y + 45, TROUGH);
        g.fill(x + 92, y + 32, x + 164, y + 33, pillColor);
        Component label = Component.translatable(menu.showArea()
                ? "screen.neroagriculture.area.hide" : "screen.neroagriculture.area.show");
        g.text(font, label, x + 128 - font.width(label) / 2, y + 35, pillColor, false);

        super.extractContents(g, mouseX, mouseY, partialTick);
    }

    @Override public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick) {
        if (event.x() >= leftPos + 92 && event.x() < leftPos + 164
                && event.y() >= topPos + 32 && event.y() < topPos + 45) {
            if (machinePos == null) {
                machinePos = za.co.neroland.neroagriculture.network.ClientMachineMenuPositions.poll(menu.containerId);
            }
            if (machinePos != null) {
                za.co.neroland.neroagriculture.platform.Services.NETWORK.sendToServer(
                        new za.co.neroland.neroagriculture.network.MachineActionPayload(machinePos.asLong(), 1, 0));
            }
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        g.text(font, title, titleLabelX, titleLabelY, TEXT, false);
        g.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, MUTED, false);
    }
}
