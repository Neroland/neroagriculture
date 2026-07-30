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

/**
 * Grow bed menu: one seed slot (auto-plants into the bed) plus energy/nutrient/tier readouts and the live
 * growth blocker for whatever is planted above the bed.
 */
public final class GrowBedMenu extends AbstractContainerMenu {
    public static final int DATA_COUNT = 5;
    /** Blocked-reason data value used when the bed has no crop above it at all. */
    public static final int NO_CROP = -1;
    private final Container machine;
    private final ContainerData data;
    private final BlockPos blockPos;

    private static final int MACHINE_SLOTS = 5;

    public GrowBedMenu(int id, Inventory inventory) {
        this(id, inventory, new SimpleContainer(MACHINE_SLOTS), new SimpleContainerData(DATA_COUNT), BlockPos.ZERO);
    }

    public GrowBedMenu(int id, Inventory inventory, Container machine, ContainerData data, BlockPos blockPos) {
        super(ModMenuTypes.GROW_BED.get(), id);
        checkContainerSize(machine, MACHINE_SLOTS);
        checkContainerDataCount(data, DATA_COUNT);
        this.machine = machine;
        this.data = data;
        this.blockPos = blockPos;
        machine.startOpen(inventory.player);
        // seed input, then four harvest-output slots (auto-harvest fills these; hoppers extract them)
        addSlot(new Slot(machine, 0, 26, 30) {
            @Override public boolean mayPlace(ItemStack stack) { return machine.canPlaceItem(0, stack); }
        });
        for (int i = 1; i < MACHINE_SLOTS; i++) {
            final int index = i;
            addSlot(new Slot(machine, index, 80 + (i - 1) * 22, 30) {
                @Override public boolean mayPlace(ItemStack stack) { return false; }
            });
        }
        // Player inventory sits 12px lower than the vanilla 176x166 layout: the screen grew to 178 tall to
        // make room for the tier + status lines under the gauges.
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 96 + row * 18));
        }
        for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, 8 + col * 18, 154));
        addDataSlots(data);
    }

    public BlockPos blockPos() { return blockPos; }
    /** Permille fraction of the live energy capacity, 0..{@link GaugeData#SCALE} (see GaugeData). */
    public int energy() { return data.get(0); }
    /** Permille fraction of the live nutrient capacity, 0..{@link GaugeData#SCALE} (see GaugeData). */
    public int nutrient() { return data.get(1); }
    public int tier() { return data.get(2); }

    /** Live growth blocker for the crop above the bed; {@code null} when nothing is planted there. */
    @org.jetbrains.annotations.Nullable
    public za.co.neroland.neroagriculture.crop.GrowthRules.BlockedReason blockedReason() {
        var reasons = za.co.neroland.neroagriculture.crop.GrowthRules.BlockedReason.values();
        int value = data.get(3);
        return value >= 0 && value < reasons.length ? reasons[value] : null;
    }

    /**
     * Growth of the crop planted above the bed as a permille of its max age,
     * 0..{@link GaugeData#SCALE}, or {@link #NO_CROP} when the bed is bare.
     */
    public int growth() { return data.get(4); }

    @Override public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack current = slot.getItem();
        ItemStack copy = current.copy();
        if (index < MACHINE_SLOTS) {
            if (!moveItemStackTo(current, MACHINE_SLOTS, slots.size(), true)) return ItemStack.EMPTY;
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
