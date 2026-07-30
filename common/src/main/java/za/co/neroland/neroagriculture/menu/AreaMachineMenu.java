package za.co.neroland.neroagriculture.menu;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import za.co.neroland.neroagriculture.registry.ModMenuTypes;

/** Planter / Harvester / Fertiliser-Applicator menu: a 3x3 seed/output grid, three upgrade slots, and mode/energy/range sync. */
public final class AreaMachineMenu extends AbstractContainerMenu {
    private static final int ITEM_SLOTS = 9;
    private static final int UPGRADE_START = 9;
    private static final int MACHINE_SLOTS = 12;
    private static final int DATA_COUNT = 4;
    private final Container machine;
    private final ContainerData data;
    private final BlockPos blockPos;

    public AreaMachineMenu(int id, Inventory inventory) {
        this(id, inventory, new SimpleContainer(MACHINE_SLOTS), new SimpleContainerData(DATA_COUNT), BlockPos.ZERO);
    }

    public AreaMachineMenu(int id, Inventory inventory, Container machine, ContainerData data, BlockPos blockPos) {
        super(ModMenuTypes.AREA_MACHINE.get(), id);
        checkContainerSize(machine, MACHINE_SLOTS);
        checkContainerDataCount(data, DATA_COUNT);
        this.machine = machine;
        this.data = data;
        this.blockPos = blockPos;
        machine.startOpen(inventory.player);
        for (int i = 0; i < ITEM_SLOTS; i++) {
            final int index = i;
            addSlot(new Slot(machine, index, 26 + (i % 3) * 18, 18 + (i / 3) * 18) {
                @Override public boolean mayPlace(ItemStack s) { return machine.canPlaceItem(index, s); }
            });
        }
        for (int i = 0; i < 3; i++) {
            final int index = UPGRADE_START + i;
            addSlot(new Slot(machine, index, 152, 18 + i * 18) {
                @Override public boolean mayPlace(ItemStack s) { return machine.canPlaceItem(index, s); }
            });
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
        }
        for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, 8 + col * 18, 142));
        addDataSlots(data);
    }

    public BlockPos blockPos() { return blockPos; }
    public int mode() { return data.get(0); }
    /** Permille fraction of the live energy capacity, 0..{@link GaugeData#SCALE} (see GaugeData). */
    public int energy() { return data.get(1); }
    public int range() { return data.get(2); }
    public boolean showArea() { return data.get(3) != 0; }

    @Override public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack current = slot.getItem();
        ItemStack copy = current.copy();
        if (index < MACHINE_SLOTS) {
            if (!moveItemStackTo(current, MACHINE_SLOTS, slots.size(), true)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(current, 0, ITEM_SLOTS, false)) {
            return ItemStack.EMPTY;
        }
        if (current.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        return copy;
    }

    @Override public boolean stillValid(Player player) { return machine.stillValid(player); }
    @Override public void removed(Player player) { super.removed(player); machine.stopOpen(player); }
}
