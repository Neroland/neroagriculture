package za.co.neroland.neroagriculture.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.neroagriculture.machine.MachineKind;
import za.co.neroland.neroagriculture.menu.FoundationMachineMenu;
import za.co.neroland.neroagriculture.network.ClientMachineMenuPositions;
import za.co.neroland.neroagriculture.network.MachineActionPayload;
import za.co.neroland.neroagriculture.platform.Services;

/** Compact procedural fabrication UI with explicit progress, energy, and blocked-state feedback. */
public final class FoundationMachineScreen extends AbstractContainerScreen<FoundationMachineMenu> {
    private static final int PANEL = 0xFF111A22;
    private static final int EDGE = 0xFF05080C;
    private static final int TRACK = 0xFF26313B;
    private static final int PROGRESS = 0xFF5CC7E8;
    private static final int ENERGY = 0xFFE0B33A;
    private static final int TEXT = 0xFFDCEEFF;
    private static final int MUTED = 0xFF93A4B5;
    @Nullable private BlockPos machinePos;

    public FoundationMachineScreen(FoundationMachineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 150);
        titleLabelX = 8;
        inventoryLabelX = 8;
        inventoryLabelY = 57;
    }

    @Override public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int x = leftPos;
        int y = topPos;
        graphics.fill(x - 1, y - 1, x + imageWidth + 1, y + imageHeight + 1, EDGE);
        graphics.fill(x, y, x + imageWidth, y + imageHeight, PANEL);
        graphics.fill(x + 8, y + 48, x + 168, y + 54, TRACK);
        int progressWidth = menu.maxProgress() <= 0 ? 0
                : Math.min(160, menu.progress() * 160 / menu.maxProgress());
        graphics.fill(x + 8, y + 48, x + 8 + progressWidth, y + 54, PROGRESS);
        int energyWidth = Math.min(160, menu.energy() * 160 / 100_000);
        graphics.fill(x + 8, y + 18, x + 168, y + 21, TRACK);
        graphics.fill(x + 8, y + 18, x + 8 + energyWidth, y + 21, ENERGY);
        Component status = Component.translatable("machine.neroagriculture.status."
                + menu.blockedReason().name().toLowerCase(java.util.Locale.ROOT));
        graphics.text(font, status, x + 82 - font.width(status) / 2, y + 38, MUTED, false);
        if (menu.machineKind() == MachineKind.RESEARCH_BENCH) {
            graphics.fill(x + 112, y + 46, x + 168, y + 58, TRACK);
            Component research = Component.translatable("screen.neroagriculture.research");
            graphics.text(font, research, x + 140 - font.width(research) / 2, y + 49, TEXT, false);
        }
        super.extractContents(graphics, mouseX, mouseY, partialTick);
    }

    @Override protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(font, title, titleLabelX, titleLabelY, TEXT, false);
        graphics.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, MUTED, false);
    }

    @Override public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (menu.machineKind() == MachineKind.RESEARCH_BENCH
                && event.x() >= leftPos + 112 && event.x() < leftPos + 168
                && event.y() >= topPos + 46 && event.y() < topPos + 58) {
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
