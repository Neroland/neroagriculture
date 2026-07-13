package za.co.neroland.neroagriculture.crop;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import za.co.neroland.neroagriculture.config.AgricultureConfig;
import za.co.neroland.neroagriculture.content.FragmentTier;
import za.co.neroland.neroagriculture.fertiliser.FertilisableBed;
import za.co.neroland.neroagriculture.fertiliser.FertiliserDose;
import za.co.neroland.neroagriculture.fertiliser.FertiliserType;
import za.co.neroland.neroagriculture.fluid.ModFluids;
import za.co.neroland.neroagriculture.registry.ModBlockEntities;
import za.co.neroland.nerolandcore.fluid.FluidBuffer;
import za.co.neroland.nerolandcore.fluid.NeroFluidStorage;
import za.co.neroland.nerolandcore.machine.AbstractMachineBlockEntity;
import za.co.neroland.nerolandcore.sideconfig.Channel;
import za.co.neroland.nerolandcore.sideconfig.SideConfig;
import za.co.neroland.nerolandcore.sideconfig.SideMode;

/** Persistent NF/nutrient storage exposed through Core's cross-loader capabilities. */
public final class GrowBedBlockEntity extends AbstractMachineBlockEntity implements FertilisableBed {
    private final FluidBuffer nutrient;
    @org.jetbrains.annotations.Nullable private FertiliserDose speedDose;
    @org.jetbrains.annotations.Nullable private FertiliserDose yieldDose;

    public GrowBedBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.GROW_BED.get(), pos, state, AgricultureConfig.MACHINE_ENERGY_CAPACITY.get(),
                AgricultureConfig.MACHINE_ENERGY_RATE.get(), 0, stack -> null);
        this.nutrient = new FluidBuffer(AgricultureConfig.MACHINE_FLUID_CAPACITY.get(), this::changedAndSync);
        installSideConfig(SideConfig.builder().channel(Channel.ENERGY).channel(Channel.FLUID)
                .allow(Channel.ENERGY, SideMode.OUTPUT, false).allow(Channel.ENERGY, SideMode.IO, false)
                .allow(Channel.FLUID, SideMode.OUTPUT, false).allow(Channel.FLUID, SideMode.IO, false).build())
                .withFluid(this::getFluid);
    }

    public FragmentTier tier() {
        return getBlockState().getBlock() instanceof GrowBedBlock bed ? bed.tier() : FragmentTier.FORGITE;
    }
    public NeroFluidStorage getFluid() { return nutrient; }
    public boolean hasGrowthResources() {
        int energyCost = AgricultureConfig.GROW_BED_ENERGY_COST.get();
        int fluidCost = AgricultureConfig.GROW_BED_NUTRIENT_COST.get();
        return GrowBedResources.has(getEnergy(), nutrient, ModFluids.NUTRIENT.get(), energyCost, fluidCost);
    }
    public boolean consumeGrowthResources() {
        if (!GrowBedResources.consume(getEnergy(), nutrient, ModFluids.NUTRIENT.get(),
                AgricultureConfig.GROW_BED_ENERGY_COST.get(), AgricultureConfig.GROW_BED_NUTRIENT_COST.get())) return false;
        changedAndSync();
        return true;
    }

    @Override public boolean applyFertiliser(FertiliserType type, int amount, long now, int durationTicks, int maxDose) {
        FertiliserDose current = type == FertiliserType.SPEED ? speedDose : yieldDose;
        FertiliserDose next = FertiliserDose.applied(current, type, amount, now, durationTicks, maxDose);
        if (type == FertiliserType.SPEED) speedDose = next; else yieldDose = next;
        changedAndSync();
        return true;
    }

    @Override public FertiliserDose activeDose(FertiliserType type, long now) {
        FertiliserDose dose = type == FertiliserType.SPEED ? speedDose : yieldDose;
        return dose != null && dose.active(now) ? dose : null;
    }

    private void changedAndSync() {
        setChanged();
    }

    @Override public void setChanged() {
        super.setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    @Override protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putString("NutrientFluid", BuiltInRegistries.FLUID.getKey(nutrient.getRawFluid()).toString());
        output.putInt("NutrientAmount", nutrient.getRawAmount());
        saveDose(output, "Speed", speedDose);
        saveDose(output, "Yield", yieldDose);
    }

    private static void saveDose(ValueOutput output, String key, @org.jetbrains.annotations.Nullable FertiliserDose dose) {
        if (dose == null) return;
        output.putInt(key + "Dose", dose.amount());
        output.putLong(key + "Expiry", dose.expiryTick());
    }
    @Override protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        Fluid fluid = BuiltInRegistries.FLUID.getValue(Identifier.parse(
                input.getStringOr("NutrientFluid", "minecraft:empty")));
        nutrient.setRaw(fluid == null ? Fluids.EMPTY : fluid, input.getIntOr("NutrientAmount", 0));
        speedDose = loadDose(input, "Speed", FertiliserType.SPEED);
        yieldDose = loadDose(input, "Yield", FertiliserType.YIELD);
    }

    @org.jetbrains.annotations.Nullable
    private static FertiliserDose loadDose(ValueInput input, String key, FertiliserType type) {
        int amount = input.getIntOr(key + "Dose", 0);
        if (amount <= 0) return null;
        return new FertiliserDose(type, amount, input.getLongOr(key + "Expiry", 0L));
    }
    @Override public Packet<ClientGamePacketListener> getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveCustomOnly(registries); }
}
