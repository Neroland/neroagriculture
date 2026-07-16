package za.co.neroland.neroagriculture.biofuel;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
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

import za.co.neroland.neroagriculture.api.AgricultureApi;
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
 * NF Biofuel Converter: farmed Biomass becomes Biofuel fluid (with an explicit loss) plus a periodic
 * recoverable Crop Waste byproduct. Surplus biofuel is offered to registered {@link AgricultureApi#BIOFUEL}
 * consumers (Nerotech/NeroPower) through the public seam — no consumer internals are imported. The one-way,
 * lossy conversion means the farm-to-fuel loop can never mint net items or energy.
 */
public final class BiofuelConverterBlockEntity extends AbstractMachineBlockEntity implements WorldlyContainer, MenuProvider {
    public static final int BIOMASS = 0;
    public static final int WASTE_OUT = 1;
    public static final int ENERGY_PER_TICK = 8;
    private static final int[] SLOTS = {0, 1};

    private final NonNullList<ItemStack> items = NonNullList.withSize(2, ItemStack.EMPTY);
    private final FluidBuffer biofuel;
    private int progress;

    private final ContainerData menuData = new ContainerData() {
        @Override public int get(int index) {
            return switch (index) {
                case 0 -> progress;
                case 1 -> AgricultureConfig.BIOFUEL_TICKS.get();
                case 2 -> (int) Math.min(Integer.MAX_VALUE, getEnergy().getAmount());
                default -> 0;
            };
        }
        @Override public void set(int index, int value) { if (index == 0) progress = value; }
        @Override public int getCount() { return za.co.neroland.neroagriculture.menu.ProcessorMenu.DATA_COUNT; }
    };

    @Override public Component getDisplayName() { return getBlockState().getBlock().getName(); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, net.minecraft.world.entity.player.Player player) {
        return new za.co.neroland.neroagriculture.menu.ProcessorMenu(
                za.co.neroland.neroagriculture.registry.ModMenuTypes.CONVERTER.get(), id, inventory, this,
                menuData, worldPosition, 1);
    }
    private int cycles;

    public BiofuelConverterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BIOFUEL_CONVERTER.get(), pos, state, AgricultureConfig.MACHINE_ENERGY_CAPACITY.get(),
                AgricultureConfig.MACHINE_ENERGY_RATE.get(), 0, stack -> null);
        this.biofuel = new FluidBuffer(AgricultureConfig.MACHINE_FLUID_CAPACITY.get(), this::setChanged);
        installSideConfig(SideConfig.builder()
                .channel(Channel.ITEM, SlotGroup.of("input", BIOMASS), SlotGroup.of("output", WASTE_OUT))
                .channel(Channel.FLUID).channel(Channel.ENERGY)
                .allow(Channel.ENERGY, SideMode.OUTPUT, false).build())
                .withItems(() -> this).withFluid(this::getFluid);
    }

    public NeroFluidStorage getFluid() { return biofuel; }

    public static void tick(Level level, BlockPos pos, BlockState state, BiofuelConverterBlockEntity machine) {
        AbstractMachineBlockEntity.tick(level, pos, state, machine);
        if (!(level instanceof ServerLevel)) return;
        machine.process();
        machine.offerBiofuel();
    }

    private void process() {
        int biomassPerCycle = AgricultureConfig.BIOFUEL_BIOMASS_PER_CYCLE.get();
        int mbPerCycle = AgricultureConfig.BIOFUEL_MB_PER_CYCLE.get();
        int capacity = AgricultureConfig.MACHINE_FLUID_CAPACITY.get();
        boolean fluidRoom = (biofuel.getRawAmount() == 0 || biofuel.getRawFluid() == ModFluids.BIOFUEL.get())
                && biofuel.getRawAmount() + mbPerCycle <= capacity;
        boolean ready = items.get(BIOMASS).getCount() >= biomassPerCycle && fluidRoom && wasteFits();
        if (!ready) {
            progress = 0;
            return;
        }
        if (getEnergy().extract(ENERGY_PER_TICK, true) < ENERGY_PER_TICK) return;
        getEnergy().extract(ENERGY_PER_TICK, false);
        if (++progress < AgricultureConfig.BIOFUEL_TICKS.get()) {
            setChanged();
            return;
        }
        progress = 0;
        items.get(BIOMASS).shrink(biomassPerCycle);
        biofuel.setRaw(ModFluids.BIOFUEL.get(), biofuel.getRawAmount() + mbPerCycle);
        cycles++;
        int wastePercent = AgricultureConfig.BIOFUEL_WASTE_PERCENT.get();
        if (wastePercent > 0 && cycles % Math.max(1, 100 / wastePercent) == 0) addWaste();
        setChanged();
    }

    /** Offer surplus biofuel to any registered consumer through the public seam and drain what is taken. */
    private void offerBiofuel() {
        int available = biofuel.getRawAmount();
        if (available <= 0 || biofuel.getRawFluid() != ModFluids.BIOFUEL.get() || AgricultureApi.BIOFUEL.isEmpty()) return;
        long energy = Biofuel.energyNf(available, AgricultureConfig.BIOFUEL_ENERGY_PER_MB.get());
        AgricultureApi.BiofuelOffer offer = new AgricultureApi.BiofuelOffer(
                BuiltInRegistries.FLUID.getKey(ModFluids.BIOFUEL.get()), available, energy);
        for (AgricultureApi.BiofuelConsumer consumer : AgricultureApi.BIOFUEL) {
            long accepted = consumer.accept(offer, false);
            if (accepted > 0) {
                biofuel.setRaw(ModFluids.BIOFUEL.get(), Math.max(0, available - (int) Math.min(available, accepted)));
                setChanged();
                return;
            }
        }
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
        output.putString("BiofuelFluid", BuiltInRegistries.FLUID.getKey(biofuel.getRawFluid()).toString());
        output.putInt("BiofuelAmount", biofuel.getRawAmount());
        output.putInt("Progress", progress);
        output.putInt("Cycles", cycles);
    }
    @Override protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        ContainerHelper.loadAllItems(input, items);
        Fluid fluid = BuiltInRegistries.FLUID.getValue(Identifier.parse(input.getStringOr("BiofuelFluid", "minecraft:empty")));
        biofuel.setRaw(fluid == null ? Fluids.EMPTY : fluid, input.getIntOr("BiofuelAmount", 0));
        progress = Math.max(0, input.getIntOr("Progress", 0));
        cycles = Math.max(0, input.getIntOr("Cycles", 0));
    }
}
