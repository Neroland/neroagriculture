package za.co.neroland.neroagriculture.client;

import java.util.List;
import java.util.Locale;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.neroagriculture.catalog.ClientMaterialCatalog;
import za.co.neroland.neroagriculture.content.FragmentTier;
import za.co.neroland.neroagriculture.content.MaterialNames;
import za.co.neroland.neroagriculture.crop.GrowthRules;
import za.co.neroland.neroagriculture.menu.GaugeData;
import za.co.neroland.neroagriculture.menu.GrowBedMenu;

/**
 * Texture-free grow bed screen: a seed slot (auto-plants into the bed), energy and nutrient gauges, the
 * bed's tier, a live "why isn't this growing" status line for the crop above it, and a scrollable
 * compatibility panel down the right-hand column listing every catalogued material split into what this
 * bed grows now and what it needs a better bed for.
 */
public final class GrowBedScreen extends AbstractContainerScreen<GrowBedMenu> {
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

    // Panel rows are rebuilt only when the synced catalog or the bed tier changes — never per frame
    // (rows() sorts up to 4096 entries, far too heavy for extractContents).
    private List<SeedCompatibility.Row> cachedRows = List.of();
    private int cachedGeneration = -1;
    @Nullable private FragmentTier cachedBedTier;
    private boolean cachedAny;

    public GrowBedScreen(GrowBedMenu menu, Inventory inventory, Component title) {
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
        // Machine column runs to x172; the vertical rule separates it from the compatibility panel.
        g.fill(x + 7, y + 83, x + MACHINE_WIDTH - 7, y + 84, DIVIDER);
        g.fill(x + MACHINE_WIDTH - 4, y + 20, x + MACHINE_WIDTH - 3, y + HEIGHT - 8, DIVIDER);

        for (Slot slot : menu.slots) {
            if (slot.x < 0 || slot.y < 0) continue;
            int sx = x + slot.x;
            int sy = y + slot.y;
            g.fill(sx - 1, sy - 1, sx + 17, sy + 17, WELL_EDGE);
            g.fill(sx, sy, sx + 16, sy + 16, WELL);
        }

        // labelled energy gauge above the slot row (synced as a fraction of the live capacity)
        Gauges.bar(g, font, x + 8, y + 18, x + 168, "Energy", menu.energy(), GaugeData.SCALE, Gauges.ENERGY);

        // seed → harvest arrow
        g.fill(x + 48, y + 37, x + 74, y + 39, DIVIDER);
        g.fill(x + 72, y + 35, x + 74, y + 41, ACCENT);

        // labelled nutrient gauge below the slot row (synced as a fraction of the live capacity)
        Gauges.bar(g, font, x + 8, y + 50, x + 168, "Nutrient", menu.nutrient(), GaugeData.SCALE, Gauges.NUTRIENT);

        FragmentTier bedTier = bedTier();
        String tier = bedTier == null ? "?" : bedTier.name();
        g.text(font, Component.literal("Tier: " + tier + " · auto-farms"), x + 8, y + 62, MUTED, false);

        // Live blocker for the crop above the bed, straight off the shared growth rules.
        GrowthRules.BlockedReason reason = menu.blockedReason();
        Component status = reason == null
                ? Component.translatable("screen.neroagriculture.bed.empty")
                : Component.translatable("machine.neroagriculture.status." + reason.name().toLowerCase(Locale.ROOT));
        int statusColor = reason == null ? MUTED
                : reason == GrowthRules.BlockedReason.NONE ? ACCENT : WARN;
        g.text(font, status, x + 8, y + 72, statusColor, false);

        drawPanel(g, x, y, bedTier);

        super.extractContents(g, mouseX, mouseY, partialTick);
    }

    private void drawPanel(GuiGraphicsExtractor g, int x, int y, @Nullable FragmentTier bedTier) {
        // Most catalogue keys are minted at runtime and can never be pre-translated, so names go through
        // the shared resolve-or-fallback rule rather than straight to Component.translatable.
        int generation = ClientMaterialCatalog.generation();
        if (!cachedAny || cachedGeneration != generation || cachedBedTier != bedTier) {
            cachedRows = SeedCompatibility.rows(ClientMaterialCatalog.entries().values(),
                    bedTier, entry -> MaterialNames.display(entry.id(), entry.displayKey()).getString());
            cachedGeneration = generation;
            cachedBedTier = bedTier;
            cachedAny = true;
        }
        List<SeedCompatibility.Row> rows = cachedRows;
        panel.draw(g, font, x + PANEL_X, y + 20, x + PANEL_X + CompatibilityPanel.WIDTH, y + HEIGHT - 8,
                Component.translatable("screen.neroagriculture.compat.title"), rows,
                List.of(Component.translatable("screen.neroagriculture.compat.accepted")),
                Component.translatable("screen.neroagriculture.compat.locked"),
                Component.translatable("screen.neroagriculture.compat.empty"));
    }

    /**
     * Bed tier as synced through the menu.
     *
     * <p>Never {@code null} in practice: the data slot is an {@code int} that starts at 0 (TERRITE) and
     * the server ships the menu's initial data slots in the same batch as the open packet, so the very
     * first frame already shows the real tier — there is no "unsynced" window worth a sentinel value for.
     * The {@code null} branch is left as a fail-closed guard for an ordinal outside
     * {@link FragmentTier#values()}, which only a desynced protocol could produce.</p>
     */
    @Nullable private FragmentTier bedTier() {
        FragmentTier[] tiers = FragmentTier.values();
        int ordinal = menu.tier();
        return ordinal >= 0 && ordinal < tiers.length ? tiers[ordinal] : null;
    }

    @Override public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        return panel.scrolled(mouseX, mouseY, scrollY) || super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        g.text(font, title, titleLabelX, titleLabelY, TEXT, false);
        g.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, MUTED, false);
    }
}
