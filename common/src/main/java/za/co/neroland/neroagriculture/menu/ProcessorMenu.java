package za.co.neroland.neroagriculture.menu;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import za.co.neroland.neroagriculture.registry.ModMenuTypes;

/**
 * Shared processing-machine menu used by the Bioreactor and Biofuel Converter (one input + one
 * output) and the Fertiliser Processor (two inputs + one output). Data slots: progress, max, energy.
 */
public final class ProcessorMenu extends AbstractContainerMenu {
    public static final int DATA_COUNT = 3;
    private final Container machine;
    private final ContainerData data;
    private final BlockPos blockPos;
    private final int inputCount;

    /** Client factory: Bioreactor / Biofuel Converter (input + output). */
    public static ProcessorMenu converter(int id, Inventory inventory) {
        return new ProcessorMenu(ModMenuTypes.CONVERTER.get(), id, inventory, new SimpleContainer(2),
                new SimpleContainerData(DATA_COUNT), BlockPos.ZERO, 1);
    }

    /** Client factory: Fertiliser Processor (two inputs + output). */
    public static ProcessorMenu processor(int id, Inventory inventory) {
        return new ProcessorMenu(ModMenuTypes.PROCESSOR.get(), id, inventory, new SimpleContainer(3),
                new SimpleContainerData(DATA_COUNT), BlockPos.ZERO, 2);
    }

    public ProcessorMenu(MenuType<?> type, int id, Inventory inventory, Container machine, ContainerData data,
            BlockPos blockPos, int inputCount) {
        super(type, id);
        checkContainerSize(machine, inputCount + 1);
        checkContainerDataCount(data, DATA_COUNT);
        this.machine = machine;
        this.data = data;
        this.blockPos = blockPos;
        this.inputCount = inputCount;
        machine.startOpen(inventory.player);
        for (int i = 0; i < inputCount; i++) {
            final int index = i;
            addSlot(new Slot(machine, index, 44 + i * 22, 30) {
                @Override public boolean mayPlace(ItemStack stack) { return machine.canPlaceItem(index, stack); }
            });
        }
        addSlot(new Slot(machine, inputCount, 116, 30) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
        });
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
        }
        for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, 8 + col * 18, 142));
        addDataSlots(data);
    }

    public BlockPos blockPos() { return blockPos; }
    // Gauge values are synced as permille fractions of the live server-side maxima (see GaugeData):
    // progress() runs 0..maxProgress() (= GaugeData.SCALE) and energy() runs 0..GaugeData.SCALE.
    public int progress() { return data.get(0); }
    public int maxProgress() { return data.get(1); }
    public int energy() { return data.get(2); }

    @Override public ItemStack quickMoveStack(Player player, int index) {
        int machineSlots = inputCount + 1;
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack current = slot.getItem();
        ItemStack copy = current.copy();
        if (index < machineSlots) {
            if (!moveItemStackTo(current, machineSlots, slots.size(), true)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(current, 0, inputCount, false)) {
            return ItemStack.EMPTY;
        }
        if (current.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        return copy;
    }

    @Override public boolean stillValid(Player player) { return machine.stillValid(player); }
    @Override public void removed(Player player) { super.removed(player); machine.stopOpen(player); }
}
