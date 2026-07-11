package za.co.neroland.neroagriculture.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.neroagriculture.config.AgricultureConfig;
import za.co.neroland.neroagriculture.registry.ModBlockEntities;
import za.co.neroland.nerolandcore.fluid.FluidBuffer;
import za.co.neroland.nerolandcore.fluid.NeroFluidStorage;
import za.co.neroland.nerolandcore.machine.AbstractMachineBlockEntity;
import za.co.neroland.nerolandcore.sideconfig.Channel;
import za.co.neroland.nerolandcore.sideconfig.SideConfig;
import za.co.neroland.nerolandcore.sideconfig.SideMode;
import za.co.neroland.nerolandcore.sideconfig.SlotGroup;

/** Persistent item/fluid/energy shell shared by the finite Stage 2 machine catalog. */
public final class FoundationMachineBlockEntity extends AbstractMachineBlockEntity implements WorldlyContainer {
    public static final int SLOT_COUNT = 4;
    private static final int[] SLOTS = {0, 1, 2, 3};
    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private final FluidBuffer fluid;

    public FoundationMachineBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FOUNDATION_MACHINE.get(), pos, state,
                AgricultureConfig.MACHINE_ENERGY_CAPACITY.get(), AgricultureConfig.MACHINE_ENERGY_RATE.get(),
                4, stack -> null);
        this.fluid = new FluidBuffer(AgricultureConfig.MACHINE_FLUID_CAPACITY.get(), this::setChanged);
        installSideConfig(SideConfig.builder()
                .channel(Channel.ITEM, SlotGroup.of("input", 0, 1), SlotGroup.of("output", 2, 3))
                .channel(Channel.FLUID)
                .channel(Channel.ENERGY)
                .allow(Channel.ENERGY, SideMode.OUTPUT, false)
                .build()).withItems(() -> this).withFluid(this::getFluid);
    }

    public NeroFluidStorage getFluid() {
        return this.fluid;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, FoundationMachineBlockEntity machine) {
        AbstractMachineBlockEntity.tick(level, pos, state, machine);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, this.items);
        output.putInt("FluidAmount", this.fluid.getRawAmount());
        output.putString("Fluid", net.minecraft.core.registries.BuiltInRegistries.FLUID
                .getKey(this.fluid.getRawFluid()).toString());
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        ContainerHelper.loadAllItems(input, this.items);
        this.fluid.setRaw(net.minecraft.core.registries.BuiltInRegistries.FLUID.getValue(
                net.minecraft.resources.Identifier.parse(input.getStringOr("Fluid", "minecraft:empty"))),
                input.getIntOr("FluidAmount", 0));
    }

    @Override public int[] getSlotsForFace(net.minecraft.core.Direction side) { return SLOTS.clone(); }
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable net.minecraft.core.Direction side) { return slot < 2; }
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, net.minecraft.core.Direction side) { return slot >= 2; }
    @Override public int getContainerSize() { return SLOT_COUNT; }
    @Override public boolean isEmpty() { return items.stream().allMatch(ItemStack::isEmpty); }
    @Override public ItemStack getItem(int slot) { return items.get(slot); }
    @Override public ItemStack removeItem(int slot, int amount) { ItemStack out = ContainerHelper.removeItem(items, slot, amount); if (!out.isEmpty()) setChanged(); return out; }
    @Override public ItemStack removeItemNoUpdate(int slot) { return ContainerHelper.takeItem(items, slot); }
    @Override public void setItem(int slot, ItemStack stack) { items.set(slot, stack); setChanged(); }
    @Override public boolean stillValid(Player player) { return !isRemoved() && player.distanceToSqr(
            getBlockPos().getX() + 0.5, getBlockPos().getY() + 0.5, getBlockPos().getZ() + 0.5) <= 64.0; }
    @Override public void clearContent() { items.clear(); setChanged(); }
}
