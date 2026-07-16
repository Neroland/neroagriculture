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

/** Grow bed menu: one seed slot (auto-plants into the bed) plus energy/nutrient/tier readouts. */
public final class GrowBedMenu extends AbstractContainerMenu {
    public static final int DATA_COUNT = 3;
    private final Container machine;
    private final ContainerData data;
    private final BlockPos blockPos;

    public GrowBedMenu(int id, Inventory inventory) {
        this(id, inventory, new SimpleContainer(1), new SimpleContainerData(DATA_COUNT), BlockPos.ZERO);
    }

    public GrowBedMenu(int id, Inventory inventory, Container machine, ContainerData data, BlockPos blockPos) {
        super(ModMenuTypes.GROW_BED.get(), id);
        checkContainerSize(machine, 1);
        checkContainerDataCount(data, DATA_COUNT);
        this.machine = machine;
        this.data = data;
        this.blockPos = blockPos;
        machine.startOpen(inventory.player);
        addSlot(new Slot(machine, 0, 80, 30) {
            @Override public boolean mayPlace(ItemStack stack) { return machine.canPlaceItem(0, stack); }
        });
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
        }
        for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, 8 + col * 18, 142));
        addDataSlots(data);
    }

    public BlockPos blockPos() { return blockPos; }
    public int energy() { return data.get(0); }
    public int nutrient() { return data.get(1); }
    public int tier() { return data.get(2); }

    @Override public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack current = slot.getItem();
        ItemStack copy = current.copy();
        if (index < 1) {
            if (!moveItemStackTo(current, 1, slots.size(), true)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(current, 0, 1, false)) {
            return ItemStack.EMPTY;
        }
        if (current.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        return copy;
    }

    @Override public boolean stillValid(Player player) { return machine.stillValid(player); }
    @Override public void removed(Player player) { super.removed(player); machine.stopOpen(player); }
}
