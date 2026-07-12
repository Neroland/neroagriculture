package za.co.neroland.neroagriculture.lifesupport;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.neroagriculture.config.AgricultureConfig;
import za.co.neroland.neroagriculture.fluid.ModFluids;
import za.co.neroland.neroagriculture.registry.ModBlockEntities;
import za.co.neroland.neroagriculture.registry.ModItems;
import za.co.neroland.nerolandcore.fluid.FluidBuffer;
import za.co.neroland.nerolandcore.fluid.NeroFluidStorage;
import za.co.neroland.nerolandcore.machine.AbstractMachineBlockEntity;
import za.co.neroland.nerolandcore.sideconfig.Channel;
import za.co.neroland.nerolandcore.sideconfig.SideConfig;
import za.co.neroland.nerolandcore.sideconfig.SideMode;
import za.co.neroland.nerolandcore.sideconfig.SlotGroup;

/**
 * NF bioreactor closing the life-support loop: farmed Biomass becomes Nutrient fluid (with an explicit loss)
 * plus a periodic recoverable Crop Waste byproduct. Losses mean the loop can never mint net items or energy.
 */
public final class BioreactorBlockEntity extends AbstractMachineBlockEntity implements WorldlyContainer {
    public static final int BIOMASS = 0;
    public static final int WASTE_OUT = 1;
    public static final int ENERGY_PER_TICK = 8;
    private static final int[] SLOTS = {0, 1};

    private final NonNullList<ItemStack> items = NonNullList.withSize(2, ItemStack.EMPTY);
    private final FluidBuffer nutrient;
    private int progress;
    private int cycles;

    public BioreactorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BIOREACTOR.get(), pos, state, AgricultureConfig.MACHINE_ENERGY_CAPACITY.get(),
                AgricultureConfig.MACHINE_ENERGY_RATE.get(), 0, stack -> null);
        this.nutrient = new FluidBuffer(AgricultureConfig.MACHINE_FLUID_CAPACITY.get(), this::setChanged);
        installSideConfig(SideConfig.builder()
                .channel(Channel.ITEM, SlotGroup.of("input", BIOMASS), SlotGroup.of("output", WASTE_OUT))
                .channel(Channel.FLUID).channel(Channel.ENERGY)
                .allow(Channel.ENERGY, SideMode.OUTPUT, false).build())
                .withItems(() -> this).withFluid(this::getFluid);
    }

    public NeroFluidStorage getFluid() { return nutrient; }

    public static void tick(Level level, BlockPos pos, BlockState state, BioreactorBlockEntity machine) {
        AbstractMachineBlockEntity.tick(level, pos, state, machine);
        if (level instanceof ServerLevel) machine.process();
    }

    private void process() {
        int biomassPerCycle = AgricultureConfig.BIOREACTOR_BIOMASS_PER_CYCLE.get();
        int nutrientPerCycle = AgricultureConfig.BIOREACTOR_NUTRIENT_MB_PER_CYCLE.get();
        int capacity = AgricultureConfig.MACHINE_FLUID_CAPACITY.get();
        boolean fluidRoom = (nutrient.getRawAmount() == 0 || nutrient.getRawFluid() == ModFluids.NUTRIENT.get())
                && nutrient.getRawAmount() + nutrientPerCycle <= capacity;
        boolean ready = items.get(BIOMASS).getCount() >= biomassPerCycle && fluidRoom && wasteFits();
        if (!ready) {
            progress = 0;
            return;
        }
        if (getEnergy().extract(ENERGY_PER_TICK, true) < ENERGY_PER_TICK) return;
        getEnergy().extract(ENERGY_PER_TICK, false);
        if (++progress < AgricultureConfig.BIOREACTOR_TICKS.get()) {
            setChanged();
            return;
        }
        progress = 0;
        items.get(BIOMASS).shrink(biomassPerCycle);
        nutrient.setRaw(ModFluids.NUTRIENT.get(), nutrient.getRawAmount() + nutrientPerCycle);
        cycles++;
        int wastePercent = AgricultureConfig.BIOREACTOR_WASTE_PERCENT.get();
        if (wastePercent > 0 && cycles % Math.max(1, 100 / wastePercent) == 0) addWaste();
        setChanged();
    }

    private boolean wasteFits() {
        ItemStack out = items.get(WASTE_OUT);
        return out.isEmpty() || out.is(ModItems.CROP_WASTE.get()) && out.getCount() < out.getMaxStackSize();
    }

    private void addWaste() {
        if (items.get(WASTE_OUT).isEmpty()) items.set(WASTE_OUT, new ItemStack(ModItems.CROP_WASTE.get()));
        else items.get(WASTE_OUT).grow(1);
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
    @Override public boolean canPlaceItem(int slot, ItemStack stack) { return slot == BIOMASS && stack.is(ModItems.BIOMASS.get()); }
    @Override public int[] getSlotsForFace(Direction side) { return SLOTS.clone(); }
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) { return canPlaceItem(slot, stack); }
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) { return slot == WASTE_OUT; }
    @Override public void clearContent() { items.clear(); setChanged(); }

    @Override protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, items);
        output.putString("NutrientFluid", BuiltInRegistries.FLUID.getKey(nutrient.getRawFluid()).toString());
        output.putInt("NutrientAmount", nutrient.getRawAmount());
        output.putInt("Progress", progress);
        output.putInt("Cycles", cycles);
    }
    @Override protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        ContainerHelper.loadAllItems(input, items);
        Fluid fluid = BuiltInRegistries.FLUID.getValue(Identifier.parse(input.getStringOr("NutrientFluid", "minecraft:empty")));
        nutrient.setRaw(fluid == null ? Fluids.EMPTY : fluid, input.getIntOr("NutrientAmount", 0));
        progress = Math.max(0, input.getIntOr("Progress", 0));
        cycles = Math.max(0, input.getIntOr("Cycles", 0));
    }
}
