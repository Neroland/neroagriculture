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

import za.co.neroland.neroagriculture.genetics.GeneticsStationBlockEntity;
import za.co.neroland.neroagriculture.registry.ModMenuTypes;

/** Genetics Station menu: two input slots (seed / seed+fragment), a locked output, and progress/energy sync. */
public final class GeneticsStationMenu extends AbstractContainerMenu {
    private static final int MACHINE_SLOTS = 3;
    private static final int DATA_COUNT = 3;
    private final Container machine;
    private final ContainerData data;
    private final BlockPos blockPos;

    public GeneticsStationMenu(int id, Inventory inventory) {
        this(id, inventory, new SimpleContainer(MACHINE_SLOTS), new SimpleContainerData(DATA_COUNT), BlockPos.ZERO);
    }

    public GeneticsStationMenu(int id, Inventory inventory, Container machine, ContainerData data, BlockPos blockPos) {
        super(ModMenuTypes.GENETICS_STATION.get(), id);
        checkContainerSize(machine, MACHINE_SLOTS);
        checkContainerDataCount(data, DATA_COUNT);
        this.machine = machine;
        this.data = data;
        this.blockPos = blockPos;
        machine.startOpen(inventory.player);
        addSlot(new Slot(machine, GeneticsStationBlockEntity.INPUT_A, 44, 32) {
            @Override public boolean mayPlace(ItemStack stack) { return machine.canPlaceItem(GeneticsStationBlockEntity.INPUT_A, stack); }
        });
        addSlot(new Slot(machine, GeneticsStationBlockEntity.INPUT_B, 66, 32) {
            @Override public boolean mayPlace(ItemStack stack) { return machine.canPlaceItem(GeneticsStationBlockEntity.INPUT_B, stack); }
        });
        addSlot(new Slot(machine, GeneticsStationBlockEntity.OUTPUT, 116, 32) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
        });
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
        }
        for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, 8 + col * 18, 142));
        addDataSlots(data);
    }

    public BlockPos blockPos() { return blockPos; }
    public int progress() { return data.get(0); }
    public int maxProgress() { return data.get(1); }
    public int energy() { return data.get(2); }

    @Override public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack current = slot.getItem();
        ItemStack copy = current.copy();
        if (index < MACHINE_SLOTS) {
            if (!moveItemStackTo(current, MACHINE_SLOTS, slots.size(), true)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(current, 0, MACHINE_SLOTS, false)) {
            return ItemStack.EMPTY;
        }
        if (current.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        return copy;
    }

    @Override public boolean stillValid(Player player) { return machine.stillValid(player); }
    @Override public void removed(Player player) { super.removed(player); machine.stopOpen(player); }
}
