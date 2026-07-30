package za.co.neroland.neroagriculture.fertiliser;

import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.neroagriculture.config.AgricultureConfig;
import za.co.neroland.neroagriculture.machine.SideConfigMigration;
import za.co.neroland.neroagriculture.registry.ModBlockEntities;
import za.co.neroland.neroagriculture.registry.ModItems;
import za.co.neroland.nerolandcore.machine.AbstractMachineBlockEntity;
import za.co.neroland.nerolandcore.sideconfig.Channel;
import za.co.neroland.nerolandcore.sideconfig.SideConfig;
import za.co.neroland.nerolandcore.sideconfig.SideMode;
import za.co.neroland.nerolandcore.sideconfig.SidePreset;
import za.co.neroland.nerolandcore.sideconfig.SlotGroup;

/** NF-powered processor: biomass + crop waste to base fertiliser, on Core machine/side-config surfaces. */
public final class FertiliserProcessorBlockEntity extends AbstractMachineBlockEntity implements WorldlyContainer, MenuProvider {
    public static final int BIOMASS = 0;
    public static final int WASTE = 1;
    public static final int OUTPUT = 2;
    public static final int PROCESS_TICKS = 100;
    public static final int ENERGY_PER_TICK = 8;
    private static final int[] SLOTS = {0, 1, 2};

    private final NonNullList<ItemStack> items = NonNullList.withSize(3, ItemStack.EMPTY);
    private int progress;

    // Gauge slots ship as permille fractions: ContainerData syncs shorts, and the configured energy
    // capacity can exceed 32,767 (see menu.GaugeData).
    private final ContainerData menuData = new ContainerData() {
        @Override public int get(int index) {
            return switch (index) {
                case 0 -> za.co.neroland.neroagriculture.menu.GaugeData.permille(progress, PROCESS_TICKS);
                case 1 -> za.co.neroland.neroagriculture.menu.GaugeData.SCALE;
                case 2 -> za.co.neroland.neroagriculture.menu.GaugeData.permille(
                        getEnergy().getAmount(), getEnergy().getCapacity());
                default -> 0;
            };
        }
        // Only the client's SimpleContainerData copy ever receives set(); the slots are scaled fractions.
        @Override public void set(int index, int value) { }
        @Override public int getCount() { return za.co.neroland.neroagriculture.menu.ProcessorMenu.DATA_COUNT; }
    };

    @Override public Component getDisplayName() { return getBlockState().getBlock().getName(); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, net.minecraft.world.entity.player.Player player) {
        return new za.co.neroland.neroagriculture.menu.ProcessorMenu(
                za.co.neroland.neroagriculture.registry.ModMenuTypes.PROCESSOR.get(), id, inventory, this,
                menuData, worldPosition, 2);
    }

    public FertiliserProcessorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FERTILISER_PROCESSOR.get(), pos, state, AgricultureConfig.MACHINE_ENERGY_CAPACITY.get(),
                AgricultureConfig.MACHINE_ENERGY_RATE.get(), 0, stack -> null);
        installSideConfig(SideConfig.builder()
                .channel(Channel.ITEM, SlotGroup.of("input", BIOMASS, WASTE), SlotGroup.of("output", OUTPUT))
                .channel(Channel.ENERGY)
                .defaultPreset(SidePreset.PROCESSOR)
                .allow(Channel.ENERGY, SideMode.OUTPUT, false).build())
                .withItems(() -> this);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, FertiliserProcessorBlockEntity machine) {
        AbstractMachineBlockEntity.tick(level, pos, state, machine);
        SideConfigMigration.tick(machine);
        if (level instanceof ServerLevel) machine.process();
    }

    private void process() {
        boolean ready = items.get(BIOMASS).getCount() >= 2 && items.get(WASTE).getCount() >= 1 && canOutput();
        if (!ready) {
            progress = 0;
            return;
        }
        if (getEnergy().extract(ENERGY_PER_TICK, true) < ENERGY_PER_TICK) return;
        getEnergy().extract(ENERGY_PER_TICK, false);
        if (++progress < PROCESS_TICKS) {
            setChanged();
            return;
        }
        progress = 0;
        items.get(BIOMASS).shrink(2);
        items.get(WASTE).shrink(1);
        if (items.get(OUTPUT).isEmpty()) items.set(OUTPUT, new ItemStack(ModItems.FERTILISER.get()));
        else items.get(OUTPUT).grow(1);
        setChanged();
    }

    private boolean canOutput() {
        ItemStack out = items.get(OUTPUT);
        return out.isEmpty() || out.is(ModItems.FERTILISER.get()) && out.getCount() < out.getMaxStackSize();
    }

    @Override public int getContainerSize() { return items.size(); }
    @Override public boolean isEmpty() { return items.stream().allMatch(ItemStack::isEmpty); }
    @Override public ItemStack getItem(int slot) { return items.get(slot); }
    @Override public ItemStack removeItem(int slot, int amount) {
        ItemStack out = ContainerHelper.removeItem(items, slot, amount);
        if (!out.isEmpty()) setChanged();
        return out;
    }
    @Override public ItemStack removeItemNoUpdate(int slot) { return ContainerHelper.takeItem(items, slot); }
    @Override public void setItem(int slot, ItemStack stack) { items.set(slot, stack); setChanged(); }
    @Override public boolean stillValid(Player player) {
        return !isRemoved() && player.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5,
                worldPosition.getZ() + 0.5) <= 64.0;
    }
    @Override public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == BIOMASS && stack.is(ModItems.BIOMASS.get())
                || slot == WASTE && stack.is(ModItems.CROP_WASTE.get());
    }
    @Override public int[] getSlotsForFace(Direction side) { return SLOTS.clone(); }
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) { return canPlaceItem(slot, stack); }
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) { return slot == OUTPUT; }
    @Override public void clearContent() { items.clear(); setChanged(); }

    /** Breaking/replacing the machine (any cause, creative and explosions included) drops the contents. */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (this.level instanceof ServerLevel) net.minecraft.world.Containers.dropContents(this.level, pos, this);
    }

    @Override protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, items);
        output.putInt("Progress", progress);
        SideConfigMigration.save(output);
    }
    @Override protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        SideConfigMigration.load(this, input);
        ContainerHelper.loadAllItems(input, items);
        progress = Math.max(0, input.getIntOr("Progress", 0));
    }
}
