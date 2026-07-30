package za.co.neroland.neroagriculture.genetics;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.neroagriculture.config.AgricultureConfig;
import za.co.neroland.neroagriculture.machine.SideConfigMigration;
import za.co.neroland.neroagriculture.menu.GeneticsStationMenu;
import za.co.neroland.neroagriculture.registry.ModBlockEntities;
import za.co.neroland.neroagriculture.registry.ModDataComponents;
import za.co.neroland.neroagriculture.registry.ModItems;
import za.co.neroland.nerolandcore.machine.AbstractMachineBlockEntity;
import za.co.neroland.nerolandcore.sideconfig.Channel;
import za.co.neroland.nerolandcore.sideconfig.SideConfig;
import za.co.neroland.nerolandcore.sideconfig.SideMode;
import za.co.neroland.nerolandcore.sideconfig.SidePreset;
import za.co.neroland.nerolandcore.sideconfig.SlotGroup;

/**
 * NF-powered Genetics Station. Two seeds splice into one (per-trait max, total-capped); a seed plus material
 * fragment upgrades the seed's lowest trait by one. All genetics maths is the deterministic, capped
 * {@link Genetics} logic, so no operation can exceed the caps.
 */
public final class GeneticsStationBlockEntity extends AbstractMachineBlockEntity implements WorldlyContainer, MenuProvider {
    public static final int INPUT_A = 0;
    public static final int INPUT_B = 1;
    public static final int OUTPUT = 2;
    public static final int PROCESS_TICKS = 120;
    public static final int ENERGY_PER_TICK = 10;
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
        @Override public int getCount() { return 3; }
    };

    public GeneticsStationBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.GENETICS_STATION.get(), pos, state, AgricultureConfig.MACHINE_ENERGY_CAPACITY.get(),
                AgricultureConfig.MACHINE_ENERGY_RATE.get(), 0, stack -> null);
        installSideConfig(SideConfig.builder()
                .channel(Channel.ITEM, SlotGroup.of("input", INPUT_A, INPUT_B), SlotGroup.of("output", OUTPUT))
                .channel(Channel.ENERGY)
                .defaultPreset(SidePreset.PROCESSOR)
                .allow(Channel.ENERGY, SideMode.OUTPUT, false).build())
                .withItems(() -> this);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, GeneticsStationBlockEntity machine) {
        AbstractMachineBlockEntity.tick(level, pos, state, machine);
        SideConfigMigration.tick(machine);
        if (level instanceof ServerLevel) machine.process();
    }

    private void process() {
        ItemStack result = plannedResult();
        if (result.isEmpty() || !canOutput(result)) {
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
        merge(result);
        items.get(INPUT_A).shrink(1);
        items.get(INPUT_B).shrink(1);
        setChanged();
    }

    /** The seed this station would output, or empty if the inputs are not a valid splice/upgrade. */
    private ItemStack plannedResult() {
        ItemStack a = items.get(INPUT_A);
        if (!isSeed(a)) return ItemStack.EMPTY;
        Genetics genA = a.getOrDefault(ModDataComponents.GENETICS.get(), Genetics.EMPTY);
        ItemStack b = items.get(INPUT_B);
        Genetics resultGenetics;
        if (isSeed(b)) {
            resultGenetics = Genetics.splice(genA, b.getOrDefault(ModDataComponents.GENETICS.get(), Genetics.EMPTY));
        } else if (b.is(ModItems.RESOURCE_FRAGMENT.get())) {
            resultGenetics = genA.upgradedLowest();
        } else {
            return ItemStack.EMPTY;
        }
        ItemStack output = a.copyWithCount(1);
        if (resultGenetics.isEmpty()) output.remove(ModDataComponents.GENETICS.get());
        else output.set(ModDataComponents.GENETICS.get(), resultGenetics);
        return output;
    }

    private boolean canOutput(ItemStack result) {
        ItemStack out = items.get(OUTPUT);
        return out.isEmpty() || ItemStack.isSameItemSameComponents(out, result) && out.getCount() < out.getMaxStackSize();
    }

    private void merge(ItemStack result) {
        if (items.get(OUTPUT).isEmpty()) items.set(OUTPUT, result);
        else items.get(OUTPUT).grow(1);
    }

    private static boolean isSeed(ItemStack stack) {
        return stack.is(ModItems.RESOURCE_SEED.get()) || stack.is(ModItems.FOOD_SEED.get())
                || stack.is(ModItems.ALIEN_SEED.get());
    }

    @Override public Component getDisplayName() { return getBlockState().getBlock().getName(); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new GeneticsStationMenu(id, inventory, this, menuData, worldPosition);
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
        return slot == INPUT_A && isSeed(stack)
                || slot == INPUT_B && (isSeed(stack) || stack.is(ModItems.RESOURCE_FRAGMENT.get()));
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
