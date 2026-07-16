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

import za.co.neroland.neroagriculture.machine.FoundationMachineBlockEntity;
import za.co.neroland.neroagriculture.machine.MachineBlockedReason;
import za.co.neroland.neroagriculture.machine.MachineKind;
import za.co.neroland.neroagriculture.registry.ModMenuTypes;

/** Five-slot fabrication menu with bounded progress, energy, status, and machine-kind sync. */
public final class FoundationMachineMenu extends AbstractContainerMenu {
    private static final int MACHINE_SLOTS = FoundationMachineBlockEntity.TOTAL_MENU_SLOTS;
    private final Container machine;
    private final ContainerData data;
    private final BlockPos blockPos;

    public FoundationMachineMenu(int id, Inventory inventory) {
        this(id, inventory, new SimpleContainer(MACHINE_SLOTS), new SimpleContainerData(5), BlockPos.ZERO);
    }

    public FoundationMachineMenu(int id, Inventory inventory, Container machine, ContainerData data,
            BlockPos blockPos) {
        super(ModMenuTypes.FOUNDATION_MACHINE.get(), id);
        checkContainerSize(machine, MACHINE_SLOTS);
        checkContainerDataCount(data, 5);
        this.machine = machine;
        this.data = data;
        this.blockPos = blockPos;
        machine.startOpen(inventory.player);
        addSlot(new Slot(machine, 0, 26, 30));
        addSlot(new Slot(machine, 1, 48, 30));
        addSlot(new Slot(machine, 2, 70, 30));
        addSlot(new Slot(machine, 3, 116, 30) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
        });
        addSlot(new Slot(machine, 4, 138, 30) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
        });
        addSlot(new Slot(machine, 5, 92, 22) {
            @Override public boolean mayPlace(ItemStack stack) { return machine.canPlaceItem(5, stack); }
        });
        addSlot(new Slot(machine, 6, 92, 42) {
            @Override public boolean mayPlace(ItemStack stack) { return machine.canPlaceItem(6, stack); }
        });
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col + row * 9 + 9,
                    8 + col * 18, 88 + row * 18));
        }
        for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, 8 + col * 18, 146));
        addDataSlots(data);
    }

    public BlockPos blockPos() { return blockPos; }
    public int progress() { return data.get(0); }
    public int maxProgress() { return data.get(1); }
    public int energy() { return data.get(2); }
    public MachineBlockedReason blockedReason() { return MachineBlockedReason.byOrdinal(data.get(3)); }
    public MachineKind machineKind() {
        int value = data.get(4);
        return value >= 0 && value < MachineKind.values().length ? MachineKind.values()[value] : MachineKind.OTHER;
    }

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
