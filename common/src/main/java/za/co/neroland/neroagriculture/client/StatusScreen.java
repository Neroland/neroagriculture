package za.co.neroland.neroagriculture.client;

import java.util.List;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import za.co.neroland.neroagriculture.menu.StatusMenu;

/**
 * Generic, texture-free status screen for the read-only controller blocks. It draws the block title, an
 * energy gauge, and up to four labelled readouts whose meaning is chosen from the synced machine id — so
 * Greenhouse/Terraforming/Beacon each get a live panel instead of a chat line, all through one screen.
 *
 * <p>For the Greenhouse Controller only, a {@link GuidePanel} build guide is bolted on to the LEFT of the
 * hull. The machine id is data-driven and can arrive after construction (and the greenhouse id happens to
 * be the pre-sync default), so the screen keeps its fixed 176-wide image and draws the guide as a joined
 * side hull outside the image bounds, per frame, only while the synced id reads greenhouse — every other
 * machine id renders exactly as before. The menu is slot-free, so nothing needs shifting.</p>
 */
public final class StatusScreen extends AbstractContainerScreen<StatusMenu> {
    private static final int HULL = 0xFFE6ECF0;
    private static final int HULL_HI = 0xFFF3F7FA;
    private static final int EDGE = 0xFF59656F;
    private static final int DIVIDER = 0xFFB4C0C8;
    private static final int ACCENT = 0xFF78C860;
    private static final int TEXT = 0xFF1E2C34;
    private static final int MUTED = 0xFF5E707C;

    // Build-guide diagram cells (a cross-section sketch): glass shell, frame base, teal door, amber controller.
    private static final int GLASS_CELL = 0xFFB8D8E0;
    private static final int FRAME_CELL = 0xFF8C9AA4;
    private static final int DOOR_CELL = 0xFF42A880;
    private static final int CONTROLLER_CELL = 0xFFE0B33A;

    /** Width of the bolted-on guide hull: the panel plus the margins mirroring the main hull's. */
    private static final int GUIDE_WIDTH = GuidePanel.WIDTH + 8;
    /**
     * Height of the guide hull — taller than the compact 108px status hull so the build guide can show
     * its steps without immediate scrolling; it extends downward as a left wing. Kept ≤200 so the whole
     * wing stays on screen at MC's largest auto GUI scale on common resolutions.
     */
    private static final int GUIDE_HEIGHT = 200;

    private final GuidePanel guide = new GuidePanel();
    private final List<GuidePanel.Row> guideRows = guideRows();
    /** Whether the last frame drew the guide — gates the wheel so a stale panel never eats scrolls. */
    private boolean guideDrawn;

    public StatusScreen(StatusMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 108);
        titleLabelX = 8;
    }

    @Override public void extractContents(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        int x = leftPos;
        int y = topPos;
        int w = imageWidth;
        int h = imageHeight;

        // Greenhouse build guide as a joined hull to the left; the main hull's 1px edge at x-1 doubles
        // as the divider between the two.
        guideDrawn = menu.value(StatusMenu.MACHINE_ID) == StatusMenu.ID_GREENHOUSE;
        if (guideDrawn) {
            int gx = x - GUIDE_WIDTH;
            g.fill(gx - 1, y - 1, x, y + GUIDE_HEIGHT + 1, EDGE);
            g.fill(gx, y, x - 1, y + GUIDE_HEIGHT, HULL);
            g.fill(gx, y, x - 1, y + 17, HULL_HI);
            g.fill(gx + 7, y + 15, x - 8, y + 16, ACCENT);
            g.fill(gx + 7, y + 16, x - 8, y + 17, DIVIDER);
            guide.draw(g, font, gx + 4, y + 20, gx + 4 + GuidePanel.WIDTH, y + GUIDE_HEIGHT - 8,
                    Component.translatable("screen.neroagriculture.guide.greenhouse.title"), guideRows);
        }

        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, EDGE);
        g.fill(x, y, x + w, y + h, HULL);
        g.fill(x, y, x + w, y + 17, HULL_HI);
        g.fill(x + 7, y + 15, x + w - 7, y + 16, ACCENT);
        g.fill(x + 7, y + 16, x + w - 7, y + 17, DIVIDER);

        // labelled energy gauge (synced as a fraction of the live capacity — see menu.GaugeData)
        Gauges.bar(g, font, x + 8, y + 20, x + 168, "Energy", menu.value(StatusMenu.ENERGY),
                za.co.neroland.neroagriculture.menu.GaugeData.SCALE, Gauges.ENERGY);

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

    /**
     * Build-guide content, stated once: what {@code GreenhouseValidation}/the controller actually do — a
     * gap-free shell of any solid blocks flood-fill checked from the controller, the Greenhouse Door
     * sealing whether open or closed, NF plus per-crop nutrient upkeep, and the configured volume cap
     * ({@code greenhouse.volume_cap}, default 4,096 — the line says so).
     */
    private static List<GuidePanel.Row> guideRows() {
        return List.of(
                GuidePanel.Row.text(Component.translatable("screen.neroagriculture.guide.greenhouse.step1")),
                GuidePanel.Row.text(Component.translatable("screen.neroagriculture.guide.greenhouse.step2")),
                GuidePanel.Row.text(Component.translatable("screen.neroagriculture.guide.greenhouse.step3")),
                GuidePanel.Row.text(Component.translatable("screen.neroagriculture.guide.greenhouse.step4")),
                GuidePanel.Row.text(Component.translatable("screen.neroagriculture.guide.greenhouse.step5")),
                GuidePanel.Row.gap(),
                GuidePanel.Row.muted(Component.translatable("screen.neroagriculture.guide.greenhouse.upkeep")),
                GuidePanel.Row.muted(Component.translatable("screen.neroagriculture.guide.greenhouse.volume")),
                GuidePanel.Row.accent(Component.translatable("screen.neroagriculture.guide.greenhouse.breach")),
                GuidePanel.Row.gap(),
                GuidePanel.Row.cells(null, GLASS_CELL, GLASS_CELL, GLASS_CELL, GLASS_CELL, GLASS_CELL),
                GuidePanel.Row.cells(Component.translatable("screen.neroagriculture.guide.greenhouse.door"),
                        GLASS_CELL, 0, 0, 0, DOOR_CELL),
                GuidePanel.Row.cells(Component.translatable("screen.neroagriculture.guide.greenhouse.controller"),
                        FRAME_CELL, FRAME_CELL, FRAME_CELL, FRAME_CELL, CONTROLLER_CELL));
    }

    private static String[] labels(int machineId) {
        return switch (machineId) {
            case StatusMenu.ID_GREENHOUSE -> new String[] {"State", "Volume", "Active crops", null};
            case StatusMenu.ID_TERRAFORMING -> new String[] {"Progress %", "Stage", null, null};
            case StatusMenu.ID_BEACON -> new String[] {"Range", null, null, null};
            default -> new String[] {"Value", null, null, null};
        };
    }

    @Override public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        // Gated on the last frame actually drawing the guide, so non-greenhouse ids are unaffected even
        // if a pre-sync frame briefly rendered it (the greenhouse id doubles as the unsynced default).
        return (guideDrawn && guide.scrolled(mouseX, mouseY, scrollY))
                || super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        g.text(font, title, titleLabelX, titleLabelY, TEXT, false);
    }
}
