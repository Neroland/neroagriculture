package za.co.neroland.neroagriculture.menu;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.neroagriculture.registry.ModMenuTypes;

/**
 * A generic, slot-free status menu shared by the read-only controller blocks (Greenhouse, Terraforming,
 * Pollination Beacon, ...). It syncs a fixed block of status integers — {@code [machineId, energy, v0,
 * v1, v2, v3]} — so a single {@link za.co.neroland.neroagriculture.client.StatusScreen} can render each
 * controller's live state instead of printing it to chat. No item slots, so nothing to quick-move.
 */
public final class StatusMenu extends AbstractContainerMenu {
    public static final int DATA_COUNT = 6;
    // machine-id values (data index 0) — pick the screen's per-line labels
    public static final int ID_GREENHOUSE = 0;
    public static final int ID_TERRAFORMING = 1;
    public static final int ID_BEACON = 2;

    public static final int MACHINE_ID = 0;
    /** Synced as a permille fraction of the live energy capacity, 0..{@link GaugeData#SCALE}. */
    public static final int ENERGY = 1;
    public static final int V0 = 2;
    public static final int V1 = 3;
    public static final int V2 = 4;
    public static final int V3 = 5;

    private final ContainerData data;
    private final BlockPos blockPos;
    @Nullable private final BlockEntity source;

    public StatusMenu(int id, Inventory inventory) {
        this(id, inventory, new SimpleContainerData(DATA_COUNT), BlockPos.ZERO, null);
    }

    public StatusMenu(int id, Inventory inventory, ContainerData data, BlockPos blockPos, @Nullable BlockEntity source) {
        super(ModMenuTypes.STATUS_CONTROLLER.get(), id);
        checkContainerDataCount(data, DATA_COUNT);
        this.data = data;
        this.blockPos = blockPos;
        this.source = source;
        addDataSlots(data);
    }

    public BlockPos blockPos() { return blockPos; }
    public int value(int index) { return data.get(index); }

    @Override public ItemStack quickMoveStack(Player player, int index) { return ItemStack.EMPTY; }

    @Override public boolean stillValid(Player player) {
        if (source == null) return true;
        return !source.isRemoved() && player.distanceToSqr(blockPos.getX() + 0.5, blockPos.getY() + 0.5,
                blockPos.getZ() + 0.5) <= 64.0;
    }
}
