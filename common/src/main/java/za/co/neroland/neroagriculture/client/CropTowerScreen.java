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
 * {@link FoundationMachineScreen}): three seed slots plus a fertiliser slot on the left, six output
 * slots on the right, an energy gauge, a nutrient gauge, a readout of tower height and active slots, a
 * live blocker line, and a scrollable seed panel down the right-hand column.
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

    /** Machine column keeps the original 176 layout; the panel column is bolted on to its right. */
    private static final int MACHINE_WIDTH = 176;
    private static final int PANEL_X = MACHINE_WIDTH;
    private static final int WIDTH = MACHINE_WIDTH + CompatibilityPanel.WIDTH + 8;
    private static final int HEIGHT = 178;

    private final CompatibilityPanel panel = new CompatibilityPanel();

    // Panel rows are rebuilt only when the synced catalog changes — never per frame (rows() sorts up
    // to 4096 entries, far too heavy for extractContents).
    private List<SeedCompatibility.Row> cachedRows = List.of();
    private int cachedGeneration = -1;
    private boolean cachedAny;

    public CropTowerScreen(CropTowerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, WIDTH, HEIGHT);
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
        g.fill(x + 7, y + 83, x + MACHINE_WIDTH - 7, y + 84, DIVIDER);
        g.fill(x + MACHINE_WIDTH - 4, y + 20, x + MACHINE_WIDTH - 3, y + HEIGHT - 8, DIVIDER);

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

        // labelled energy + nutrient gauges, side by side under the slots (synced as capacity fractions)
        Gauges.bar(g, font, x + 8, y + 59, x + 84, "Energy", menu.energy(), GaugeData.SCALE, Gauges.ENERGY);
        Gauges.bar(g, font, x + 88, y + 59, x + 168, "Nutrient", menu.nutrient(), GaugeData.SCALE, Gauges.NUTRIENT);

        // Compact status beside the fertiliser slot, left of the output cluster (x115+).
        Component tower = menu.height() <= 0
                ? Component.translatable("screen.neroagriculture.tower.unformed")
                : Component.literal("H" + menu.height() + " · " + menu.activeSlots() + " slots");
        g.text(font, tower, x + 48, y + 47, MUTED, false);

        // Aggregate blocker for the tower's planted slots; nothing planted reads as idle.
        GrowthRules.BlockedReason reason = menu.blockedReason();
        Component status = reason == null
                ? Component.translatable("screen.neroagriculture.tower.idle")
                : Component.translatable("machine.neroagriculture.status." + reason.name().toLowerCase(Locale.ROOT));
        int statusColor = reason == null ? MUTED
                : reason == GrowthRules.BlockedReason.NONE ? ACCENT : WARN;
        g.text(font, status, x + 8, y + 72, statusColor, false);

        drawPanel(g, x, y);

        super.extractContents(g, mouseX, mouseY, partialTick);
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
        return panel.scrolled(mouseX, mouseY, scrollY) || super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        g.text(font, title, titleLabelX, titleLabelY, TEXT, false);
        g.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, MUTED, false);
    }
}
