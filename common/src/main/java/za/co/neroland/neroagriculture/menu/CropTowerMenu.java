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

/** Crop Tower menu: three seed slots + a fertiliser slot feed the virtual tower; six output slots collect fragments. */
public final class CropTowerMenu extends AbstractContainerMenu {
    private static final int MACHINE_SLOTS = 10;
    private static final int OUTPUT_START = 4;
    private static final int DATA_COUNT = 4;
    private final Container machine;
    private final ContainerData data;
    private final BlockPos blockPos;

    public CropTowerMenu(int id, Inventory inventory) {
        this(id, inventory, new SimpleContainer(MACHINE_SLOTS), new SimpleContainerData(DATA_COUNT), BlockPos.ZERO);
    }

    public CropTowerMenu(int id, Inventory inventory, Container machine, ContainerData data, BlockPos blockPos) {
        super(ModMenuTypes.CROP_TOWER_CONTROLLER.get(), id);
        checkContainerSize(machine, MACHINE_SLOTS);
        checkContainerDataCount(data, DATA_COUNT);
        this.machine = machine;
        this.data = data;
        this.blockPos = blockPos;
        machine.startOpen(inventory.player);
        // three seed slots + a fertiliser slot (inputs)
        addSlot(new Slot(machine, 0, 26, 20) { @Override public boolean mayPlace(ItemStack s) { return machine.canPlaceItem(0, s); } });
        addSlot(new Slot(machine, 1, 44, 20) { @Override public boolean mayPlace(ItemStack s) { return machine.canPlaceItem(1, s); } });
        addSlot(new Slot(machine, 2, 62, 20) { @Override public boolean mayPlace(ItemStack s) { return machine.canPlaceItem(2, s); } });
        addSlot(new Slot(machine, 3, 26, 44) { @Override public boolean mayPlace(ItemStack s) { return machine.canPlaceItem(3, s); } });
        // six output slots (locked)
        int[][] out = {{116, 20}, {134, 20}, {152, 20}, {116, 38}, {134, 38}, {152, 38}};
        for (int i = 0; i < 6; i++) {
            final int slotIndex = OUTPUT_START + i;
            addSlot(new Slot(machine, slotIndex, out[i][0], out[i][1]) { @Override public boolean mayPlace(ItemStack s) { return false; } });
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
        }
        for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, 8 + col * 18, 142));
        addDataSlots(data);
    }

    public BlockPos blockPos() { return blockPos; }
    public int height() { return data.get(0); }
    public int activeSlots() { return data.get(1); }
    public int energy() { return data.get(2); }
    public int nutrient() { return data.get(3); }

    @Override public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack current = slot.getItem();
        ItemStack copy = current.copy();
        if (index < MACHINE_SLOTS) {
            if (!moveItemStackTo(current, MACHINE_SLOTS, slots.size(), true)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(current, 0, OUTPUT_START, false)) {
            return ItemStack.EMPTY;
        }
        if (current.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        return copy;
    }

    @Override public boolean stillValid(Player player) { return machine.stillValid(player); }
    @Override public void removed(Player player) { super.removed(player); machine.stopOpen(player); }
}
