package za.co.neroland.neroagriculture.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import za.co.neroland.neroagriculture.menu.StatusMenu;

/**
 * Generic, texture-free status screen for the read-only controller blocks. It draws the block title, an
 * energy gauge, and up to four labelled readouts whose meaning is chosen from the synced machine id — so
 * Greenhouse/Terraforming/Beacon each get a live panel instead of a chat line, all through one screen.
 */
public final class StatusScreen extends AbstractContainerScreen<StatusMenu> {
    private static final int HULL = 0xFFE6ECF0;
    private static final int HULL_HI = 0xFFF3F7FA;
    private static final int EDGE = 0xFF59656F;
    private static final int DIVIDER = 0xFFB4C0C8;
    private static final int ACCENT = 0xFF78C860;
    private static final int TEXT = 0xFF1E2C34;
    private static final int MUTED = 0xFF5E707C;

    public StatusScreen(StatusMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 108);
        titleLabelX = 8;
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

        // labelled energy gauge
        Gauges.bar(g, font, x + 8, y + 20, x + 168, "Energy", menu.value(StatusMenu.ENERGY), 100_000, Gauges.ENERGY);

        // per-machine labelled readouts
        String[] labels = labels(menu.value(StatusMenu.MACHINE_ID));
        int[] indices = {StatusMenu.V0, StatusMenu.V1, StatusMenu.V2, StatusMenu.V3};
        int line = 0;
        for (int i = 0; i < labels.length && i < indices.length; i++) {
            if (labels[i] == null) continue;
            Component c = Component.literal(labels[i] + ": " + menu.value(indices[i]));
            g.text(font, c, x + 8, y + 36 + line * 11, MUTED, false);
            line++;
        }

        super.extractContents(g, mouseX, mouseY, partialTick);
    }

    private static String[] labels(int machineId) {
        return switch (machineId) {
            case StatusMenu.ID_GREENHOUSE -> new String[] {"State", "Volume", "Active crops", null};
            case StatusMenu.ID_TERRAFORMING -> new String[] {"Progress %", "Stage", null, null};
            case StatusMenu.ID_BEACON -> new String[] {"Range", null, null, null};
            default -> new String[] {"Value", null, null, null};
        };
    }

    @Override protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        g.text(font, title, titleLabelX, titleLabelY, TEXT, false);
    }
}
