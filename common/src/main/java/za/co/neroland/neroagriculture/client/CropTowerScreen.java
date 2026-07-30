package za.co.neroland.neroagriculture.client;

import java.util.List;
import java.util.Locale;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

import za.co.neroland.neroagriculture.catalog.ClientMaterialCatalog;
import za.co.neroland.neroagriculture.content.MaterialNames;
import za.co.neroland.neroagriculture.crop.GrowthRules;
import za.co.neroland.neroagriculture.menu.CropTowerMenu;
import za.co.neroland.neroagriculture.menu.GaugeData;

/**
 * Texture-free screen for the Crop Tower Controller (drawn with {@code fill}/{@code text} like
 * {@link FoundationMachineScreen}): a build-guide panel down the left-hand column, then the machine
 * column — three seed slots plus a fertiliser slot, six output slots, an energy gauge, a nutrient gauge,
 * the growth bar with the tower height/slots readout beside it, and a live blocker line, each on its own
 * row — and a scrollable seed panel down the right-hand column.
 *
 * <p>The tower has no bed, so it has <em>no tier gate</em> — every resource seed it accepts is a seed it
 * will grow. The panel therefore lists the whole catalogue as growable and says where the gate actually
 * bites: progression gates, NF and nutrient are checked at growth time, not at insert time.</p>
 */
public final class CropTowerScreen extends AbstractContainerScreen<CropTowerMenu> {
    private static final int HULL = 0xFFE6ECF0;
    private static final int HULL_HI = 0xFFF3F7FA;
    private static final int EDGE = 0xFF59656F;
    private static final int DIVIDER = 0xFFB4C0C8;
    private static final int WELL = 0xFF9AA6AE;
    private static final int WELL_EDGE = 0xFF3A444C;
    private static final int ACCENT = 0xFF78C860;
    private static final int WARN = 0xFFB4442E;
    private static final int TEXT = 0xFF1E2C34;
    private static final int MUTED = 0xFF5E707C;

    // Build-guide diagram cells (a sketch, not pixel art): steel casing frames over an amber controller.
    private static final int FRAME_CELL = 0xFF8C9AA4;
    private static final int CONTROLLER_CELL = 0xFFE0B33A;

    /**
     * Three columns: the guide panel on the left, the original 176-wide machine layout in the middle
     * (every slot and drawing x-offset shifted by {@link CropTowerMenu#MACHINE_X}), and the seed panel
     * on the right.
     */
    private static final int MACHINE_X = CropTowerMenu.MACHINE_X;
    private static final int MACHINE_WIDTH = 176;
    private static final int GUIDE_X = 8;
    private static final int PANEL_X = MACHINE_X + MACHINE_WIDTH;
    private static final int WIDTH = PANEL_X + CompatibilityPanel.WIDTH + 8;
    private static final int HEIGHT = 192;

    private final CompatibilityPanel panel = new CompatibilityPanel();
    private final GuidePanel guide = new GuidePanel();
    private final List<GuidePanel.Row> guideRows = guideRows();

    // Panel rows are rebuilt only when the synced catalog changes — never per frame (rows() sorts up
    // to 4096 entries, far too heavy for extractContents).
    private List<SeedCompatibility.Row> cachedRows = List.of();
    private int cachedGeneration = -1;
    private boolean cachedAny;

    public CropTowerScreen(CropTowerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, WIDTH, HEIGHT);
        titleLabelX = MACHINE_X + 8;
        inventoryLabelX = MACHINE_X + 8;
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
        // horizontal rule between the machine rows and the player inventory
        g.fill(x + MACHINE_X + 7, y + 95, x + MACHINE_X + MACHINE_WIDTH - 7, y + 96, DIVIDER);
        // vertical rules boxing the machine column: guide panel left, seed panel right
        g.fill(x + MACHINE_X + 3, y + 20, x + MACHINE_X + 4, y + HEIGHT - 8, DIVIDER);
        g.fill(x + MACHINE_X + MACHINE_WIDTH - 4, y + 20, x + MACHINE_X + MACHINE_WIDTH - 3, y + HEIGHT - 8, DIVIDER);

        for (Slot slot : menu.slots) {
            if (slot.x < 0 || slot.y < 0) continue;
            int sx = x + slot.x;
            int sy = y + slot.y;
            g.fill(sx - 1, sy - 1, sx + 17, sy + 17, WELL_EDGE);
            g.fill(sx, sy, sx + 16, sy + 16, WELL);
        }

        // feed arrow toward the output cluster
        g.fill(x + MACHINE_X + 86, y + 27, x + MACHINE_X + 114, y + 28, DIVIDER);
        g.fill(x + MACHINE_X + 112, y + 25, x + MACHINE_X + 114, y + 30, ACCENT);

        // labelled energy + nutrient gauges, side by side under the slots (synced as capacity fractions)
        Gauges.bar(g, font, x + MACHINE_X + 8, y + 59, x + MACHINE_X + 84, "Energy", menu.energy(), GaugeData.SCALE, Gauges.ENERGY);
        Gauges.bar(g, font, x + MACHINE_X + 88, y + 59, x + MACHINE_X + 168, "Nutrient", menu.nutrient(), GaugeData.SCALE, Gauges.NUTRIENT);

        // Average growth across the planted slots as a mini labelled bar (fill = average age, label =
        // ripe count), always drawn on its own row: an empty tower shows the bare bar with the em-dash
        // label, mirroring the grow bed's idiom. Synced as a permille of max age (see GaugeData).
        int growthAvg = menu.growthAvg();
        String growthLabel = (growthAvg < 0
                ? Component.translatable("screen.neroagriculture.growth.none")
                : Component.translatable("screen.neroagriculture.tower.ripe", menu.matureSlots(), menu.plantedSlots()))
                .getString();
        Gauges.bar(g, font, x + MACHINE_X + 8, y + 71, x + MACHINE_X + 84, growthLabel,
                Math.max(0, growthAvg), GaugeData.SCALE, Gauges.PROGRESS);

        // Tower readout beside the growth bar, aligned with its label text.
        Component tower = menu.height() <= 0
                ? Component.translatable("screen.neroagriculture.tower.unformed")
                : Component.literal("H" + menu.height() + " · " + menu.activeSlots() + " slots");
        g.text(font, tower, x + MACHINE_X + 88, y + 72, MUTED, false);

        // Aggregate blocker for the tower's planted slots on its own full-width row; nothing planted
        // reads as idle.
        GrowthRules.BlockedReason reason = menu.blockedReason();
        Component status = reason == null
                ? Component.translatable("screen.neroagriculture.tower.idle")
                : Component.translatable("machine.neroagriculture.status." + reason.name().toLowerCase(Locale.ROOT));
        int statusColor = reason == null ? MUTED
                : reason == GrowthRules.BlockedReason.NONE ? ACCENT : WARN;
        g.text(font, status, x + MACHINE_X + 8, y + 83, statusColor, false);

        guide.draw(g, font, x + GUIDE_X, y + 20, x + GUIDE_X + GuidePanel.WIDTH, y + HEIGHT - 8,
                Component.translatable("screen.neroagriculture.guide.tower.title"), guideRows);
        drawPanel(g, x, y);

        super.extractContents(g, mouseX, mouseY, partialTick);
    }

    /**
     * Build-guide content, stated once: what the controller's revalidation actually does — a contiguous
     * run of Crop Tower Frames directly above the controller, formed automatically on the slow recheck
     * interval, fed through any face via side configuration. The 3/12/4 numbers are the config defaults
     * ({@code crop_tower.min_height}/{@code max_height}/{@code slots_per_layer}) and the lines say so.
     */
    private static List<GuidePanel.Row> guideRows() {
        return List.of(
                GuidePanel.Row.text(Component.translatable("screen.neroagriculture.guide.tower.step1")),
                GuidePanel.Row.text(Component.translatable("screen.neroagriculture.guide.tower.step2")),
                GuidePanel.Row.text(Component.translatable("screen.neroagriculture.guide.tower.step3")),
                GuidePanel.Row.text(Component.translatable("screen.neroagriculture.guide.tower.step4")),
                GuidePanel.Row.text(Component.translatable("screen.neroagriculture.guide.tower.step5")),
                GuidePanel.Row.gap(),
                GuidePanel.Row.accent(Component.translatable("screen.neroagriculture.guide.tower.capacity")),
                GuidePanel.Row.gap(),
                GuidePanel.Row.cells(Component.translatable("screen.neroagriculture.guide.tower.frame"), FRAME_CELL),
                GuidePanel.Row.cells(null, FRAME_CELL),
                GuidePanel.Row.cells(null, FRAME_CELL),
                GuidePanel.Row.cells(Component.translatable("screen.neroagriculture.guide.tower.controller"), CONTROLLER_CELL));
    }

    /**
     * The tower has no bed tier, so a {@code null} bed tier is passed and every material is listed as
     * growable; the note under the title spells out that gates and resources still apply at growth time.
     */
    private void drawPanel(GuiGraphicsExtractor g, int x, int y) {
        // Most catalogue keys are minted at runtime and can never be pre-translated, so names go through
        // the shared resolve-or-fallback rule rather than straight to Component.translatable.
        int generation = ClientMaterialCatalog.generation();
        if (!cachedAny || cachedGeneration != generation) {
            cachedRows = SeedCompatibility.rows(ClientMaterialCatalog.entries().values(),
                    null, entry -> MaterialNames.display(entry.id(), entry.displayKey()).getString());
            cachedGeneration = generation;
            cachedAny = true;
        }
        List<SeedCompatibility.Row> rows = cachedRows;
        panel.draw(g, font, x + PANEL_X, y + 20, x + PANEL_X + CompatibilityPanel.WIDTH, y + HEIGHT - 8,
                Component.translatable("screen.neroagriculture.compat.tower_title"), rows,
                List.of(Component.translatable("screen.neroagriculture.compat.tower_any_tier"),
                        Component.translatable("screen.neroagriculture.compat.tower_gate")),
                null, Component.translatable("screen.neroagriculture.compat.empty"));
    }

    @Override public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        return guide.scrolled(mouseX, mouseY, scrollY) || panel.scrolled(mouseX, mouseY, scrollY)
                || super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        g.text(font, title, titleLabelX, titleLabelY, TEXT, false);
        g.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, MUTED, false);
    }
}
