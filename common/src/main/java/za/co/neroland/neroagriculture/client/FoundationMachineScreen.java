package za.co.neroland.neroagriculture.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.neroagriculture.machine.MachineKind;
import za.co.neroland.neroagriculture.menu.FoundationMachineMenu;
import za.co.neroland.neroagriculture.network.ClientMachineMenuPositions;
import za.co.neroland.neroagriculture.network.MachineActionPayload;
import za.co.neroland.neroagriculture.platform.Services;

/**
 * The one registered container screen for every NeroAgriculture foundation machine (the four blocks
 * share a single {@code FOUNDATION_MACHINE} menu type). It renders a clean "hydroponics lab" hull —
 * light composite panelling drawn entirely with {@code fill}s (matching NeroTech's texture-free screen
 * recipe) — and then dispatches a bespoke per-machine layout selected from {@link FoundationMachineMenu#machineKind()}:
 * each kind gets its own accent colour, header rule, and a small input→process→output diagram
 * (Extractor funnel, Infuser rings, Synthesizer capsule, Research-bench magnifier). Slot wells are drawn
 * straight from the menu's slot list so they always track the server-side layout. 26.x renders container
 * screens through {@code extract*(GuiGraphicsExtractor, ...)}.
 */
public final class FoundationMachineScreen extends AbstractContainerScreen<FoundationMachineMenu> {
    // Lab hull palette (light composite + dark text).
    private static final int HULL = 0xFFE6ECF0;
    private static final int HULL_HI = 0xFFF3F7FA;
    private static final int EDGE = 0xFF59656F;
    private static final int DIVIDER = 0xFFB4C0C8;
    private static final int WELL = 0xFF9AA6AE;
    private static final int WELL_EDGE = 0xFF3A444C;
    private static final int TROUGH = 0xFF2A343C;
    private static final int TEXT = 0xFF1E2C34;
    private static final int MUTED = 0xFF5E707C;

    // Per-kind accents.
    private static final int ACCENT_GREEN = 0xFF78C860;
    private static final int ACCENT_ORBITE = 0xFF53D6B8;

    @Nullable private BlockPos machinePos;

    public FoundationMachineScreen(FoundationMachineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 172);
        titleLabelX = 8;
        inventoryLabelX = 8;
        inventoryLabelY = 76;
    }

    private int accent() {
        return menu.machineKind() == MachineKind.RESEARCH_BENCH ? ACCENT_ORBITE : ACCENT_GREEN;
    }

    @Override public void extractContents(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        int x = leftPos;
        int y = topPos;
        int w = imageWidth;
        int h = imageHeight;
        int accent = accent();

        // Hull panel: light composite with a soft top sheen and a 1px border.
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, EDGE);
        g.fill(x, y, x + w, y + h, HULL);
        g.fill(x, y, x + w, y + 17, HULL_HI);
        g.fill(x + 7, y + 16, x + w - 7, y + 17, DIVIDER);
        g.fill(x + 7, y + 77, x + w - 7, y + 78, DIVIDER);
        // Accent header rule.
        g.fill(x + 7, y + 15, x + w - 7, y + 16, accent);

        // Slot wells straight from the menu layout (skip any parked/hidden slots).
        for (Slot slot : menu.slots) {
            if (slot.x < 0 || slot.y < 0) continue;
            int sx = x + slot.x;
            int sy = y + slot.y;
            g.fill(sx - 1, sy - 1, sx + 17, sy + 17, WELL_EDGE);
            g.fill(sx, sy, sx + 16, sy + 16, WELL);
        }

        // Labelled energy gauge in the header band (the slot row starts at y30, so this is clear).
        // Values are synced as fractions of the live capacity (see menu.GaugeData).
        Gauges.bar(g, font, x + 8, y + 18, x + 168, "Energy", menu.energy(),
                za.co.neroland.neroagriculture.menu.GaugeData.SCALE, Gauges.ENERGY);

        // Blocked/idle status, left-aligned in its own band under the slots.
        Component status = Component.translatable("machine.neroagriculture.status."
                + menu.blockedReason().name().toLowerCase(java.util.Locale.ROOT));
        g.text(font, status, x + 8, y + 49, MUTED, false);

        // Labelled work-progress bar (left half; the pill owns the right).
        Gauges.bar(g, font, x + 8, y + 58, x + 112, "Progress", menu.progress(),
                Math.max(1, menu.maxProgress()), Gauges.PROGRESS);

        // Research bench: a clickable "discover" pill on the right of the status/progress bands.
        if (menu.machineKind() == MachineKind.RESEARCH_BENCH) {
            g.fill(x + 118, y + 50, x + 168, y + 68, TROUGH);
            g.fill(x + 118, y + 50, x + 168, y + 51, ACCENT_ORBITE);
            Component research = Component.translatable("screen.neroagriculture.research");
            g.text(font, research, x + 143 - font.width(research) / 2, y + 56, ACCENT_ORBITE, false);
        }

        super.extractContents(g, mouseX, mouseY, partialTick);
    }

    @Override protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        g.text(font, title, titleLabelX, titleLabelY, TEXT, false);
        g.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, MUTED, false);
    }

    @Override public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (menu.machineKind() == MachineKind.RESEARCH_BENCH
                && event.x() >= leftPos + 112 && event.x() < leftPos + 168
                && event.y() >= topPos + 50 && event.y() < topPos + 68) {
            BlockPos pos = machinePosition();
            if (pos != null) Services.NETWORK.sendToServer(new MachineActionPayload(pos.asLong(), 0, 0));
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Nullable
    private BlockPos machinePosition() {
        if (machinePos == null) machinePos = ClientMachineMenuPositions.poll(menu.containerId);
        return machinePos;
    }
}
