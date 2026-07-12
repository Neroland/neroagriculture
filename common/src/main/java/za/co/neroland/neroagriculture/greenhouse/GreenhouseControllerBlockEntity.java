package za.co.neroland.neroagriculture.greenhouse;

import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.neroagriculture.config.AgricultureConfig;
import za.co.neroland.neroagriculture.crop.ResourceCropBlock;
import za.co.neroland.neroagriculture.crop.SpeciesCropBlock;
import za.co.neroland.neroagriculture.fluid.ModFluids;
import za.co.neroland.neroagriculture.registry.ModBlockEntities;
import za.co.neroland.nerolandcore.fluid.FluidBuffer;
import za.co.neroland.nerolandcore.fluid.NeroFluidStorage;
import za.co.neroland.nerolandcore.machine.AbstractMachineBlockEntity;
import za.co.neroland.nerolandcore.sideconfig.Channel;
import za.co.neroland.nerolandcore.sideconfig.SideConfig;
import za.co.neroland.nerolandcore.sideconfig.SideMode;

/** NF/nutrient-powered controller that maintains a sealed, cached interior and publishes it to the index. */
public final class GreenhouseControllerBlockEntity extends AbstractMachineBlockEntity {
    private final FluidBuffer nutrient;
    private GreenhouseState state = GreenhouseState.UNFORMED;
    private Set<Long> interior = Set.of();
    private int volume;
    private int activeCrops;
    private int oxygenOffset;
    @Nullable private BlockPos leak;
    private int revalidateTimer;
    private int upkeepTimer;

    public GreenhouseControllerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.GREENHOUSE_CONTROLLER.get(), pos, state, AgricultureConfig.MACHINE_ENERGY_CAPACITY.get(),
                AgricultureConfig.MACHINE_ENERGY_RATE.get(), 0, stack -> null);
        this.nutrient = new FluidBuffer(AgricultureConfig.MACHINE_FLUID_CAPACITY.get(), this::setChanged);
        installSideConfig(SideConfig.builder().channel(Channel.ENERGY).channel(Channel.FLUID)
                .allow(Channel.ENERGY, SideMode.OUTPUT, false).allow(Channel.ENERGY, SideMode.IO, false)
                .allow(Channel.FLUID, SideMode.OUTPUT, false).allow(Channel.FLUID, SideMode.IO, false).build())
                .withFluid(this::getFluid);
    }

    public NeroFluidStorage getFluid() { return nutrient; }
    public GreenhouseState state() { return state; }
    public int volume() { return volume; }
    public int activeCrops() { return activeCrops; }
    public int oxygen() { return oxygenOffset; }
    @Nullable public BlockPos leak() { return leak; }
    public boolean isActive() { return state == GreenhouseState.FORMED; }

    public static void tick(Level level, BlockPos pos, BlockState blockState, GreenhouseControllerBlockEntity be) {
        AbstractMachineBlockEntity.tick(level, pos, blockState, be);
        if (!(level instanceof ServerLevel serverLevel)) return;
        if (--be.revalidateTimer <= 0) {
            be.revalidateTimer = AgricultureConfig.GREENHOUSE_REVALIDATE_TICKS.get();
            be.revalidate(serverLevel);
        }
        if (--be.upkeepTimer <= 0) {
            be.upkeepTimer = AgricultureConfig.GREENHOUSE_UPKEEP_TICKS.get();
            be.upkeep(serverLevel);
        }
    }

    private void revalidate(ServerLevel level) {
        GreenhouseValidation.Result result = GreenhouseValidation.validate(worldPosition,
                pos -> passable(level, pos), AgricultureConfig.GREENHOUSE_VOLUME_CAP.get());
        this.volume = result.volume();
        this.leak = result.leak();
        if (result.structure() == GreenhouseValidation.Structure.FORMED) {
            this.interior = result.interior();
            this.activeCrops = countCrops(level, result.interior());
            this.oxygenOffset = sumOxygen(level, result.interior());
        } else {
            this.interior = Set.of();
            this.activeCrops = 0;
            this.oxygenOffset = 0;
            setStructuralState(result.structure() == GreenhouseValidation.Structure.BREACHED
                    ? GreenhouseState.BREACHED : GreenhouseState.UNFORMED);
            GreenhouseIndex.clear(level, worldPosition);
        }
        setChanged();
    }

    private void upkeep(ServerLevel level) {
        if (interior.isEmpty()) {
            GreenhouseIndex.clear(level, worldPosition);
            return;
        }
        int nutrientDemand = AgricultureConfig.GREENHOUSE_NUTRIENT_PER_CROP.get() * activeCrops;
        int oxygenForNutrient = Math.min(oxygenOffset, nutrientDemand);
        int nutrientCost = nutrientDemand - oxygenForNutrient;
        int leftoverOxygen = oxygenOffset - oxygenForNutrient;
        int baseNf = AgricultureConfig.GREENHOUSE_NF_PER_VOLUME.get() * Math.max(1, (volume + 31) / 32);
        int nfCost = Math.max(0, baseNf - leftoverOxygen * AgricultureConfig.GREENHOUSE_OXYGEN_NF_FACTOR.get());
        boolean nutrientOk = nutrientCost == 0
                || nutrient.getFluid() == ModFluids.NUTRIENT.get() && nutrient.drain(nutrientCost, true) >= nutrientCost;
        boolean powerOk = nfCost == 0 || getEnergy().extract(nfCost, true) >= nfCost;
        if (powerOk && nutrientOk) {
            if (nfCost > 0) getEnergy().extract(nfCost, false);
            if (nutrientCost > 0) nutrient.drain(nutrientCost, false);
            setStructuralState(GreenhouseState.FORMED);
            GreenhouseIndex.publish(level, worldPosition, interior);
        } else {
            setStructuralState(GreenhouseState.UNPOWERED);
            GreenhouseIndex.clear(level, worldPosition);
        }
    }

    private void setStructuralState(GreenhouseState next) {
        if (state != next) {
            state = next;
            setChanged();
        }
    }

    private static boolean passable(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.isAir() || state.canBeReplaced()
                || state.getBlock() instanceof ResourceCropBlock || state.getBlock() instanceof SpeciesCropBlock;
    }

    private static int countCrops(ServerLevel level, Set<Long> interior) {
        int count = 0;
        for (long key : interior) {
            var block = level.getBlockState(BlockPos.of(key)).getBlock();
            if (block instanceof ResourceCropBlock || block instanceof SpeciesCropBlock) count++;
        }
        return count;
    }

    /**
     * Oxygen produced by interior crops — oxygen flora scaled by maturity plus every crop's oxygen genetics —
     * clamped to a hard per-volume cap. This offsets the greenhouse's nutrient and NF upkeep (a cost reducer;
     * it never gates growth) and is published to the Nerospace seam for atmosphere/terraforming.
     */
    private int sumOxygen(ServerLevel level, Set<Long> interior) {
        int total = 0;
        for (long key : interior) {
            BlockPos cropPos = BlockPos.of(key);
            var be = level.getBlockEntity(cropPos);
            if (be instanceof za.co.neroland.neroagriculture.crop.ResourceCropBlockEntity crop) {
                total += crop.genetics().oxygenOutput();
            } else if (be instanceof za.co.neroland.neroagriculture.crop.SpeciesCropBlockEntity crop) {
                int production = za.co.neroland.neroagriculture.food.FoodCatalog.lookup(level.getServer(), crop.species())
                        .map(za.co.neroland.neroagriculture.food.FoodDefinition::oxygenProduction).orElse(0);
                var state = level.getBlockState(cropPos);
                int age = state.getBlock() instanceof za.co.neroland.neroagriculture.crop.SpeciesCropBlock
                        ? state.getValue(za.co.neroland.neroagriculture.crop.SpeciesCropBlock.AGE) : 0;
                total += za.co.neroland.neroagriculture.environment.OxygenContribution.perCrop(production, age,
                        za.co.neroland.neroagriculture.crop.SpeciesCropBlock.MAX_AGE, crop.genetics().oxygenOutput());
            }
        }
        int capped = za.co.neroland.neroagriculture.environment.OxygenContribution.capped(total, volume,
                AgricultureConfig.GREENHOUSE_OXYGEN_CAP_PER_32.get(), 32);
        za.co.neroland.neroagriculture.environment.OxygenApi.contribute(
                new za.co.neroland.neroagriculture.environment.OxygenApi.Contribution(
                        level.dimension().identifier(), worldPosition.asLong(), capped));
        return capped;
    }

    @Override public void setRemoved() {
        if (level instanceof ServerLevel serverLevel) GreenhouseIndex.clear(serverLevel, worldPosition);
        super.setRemoved();
    }

    @Override protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putString("NutrientFluid", BuiltInRegistries.FLUID.getKey(nutrient.getRawFluid()).toString());
        output.putInt("NutrientAmount", nutrient.getRawAmount());
        output.putInt("GreenhouseState", state.ordinal());
        output.putInt("Volume", volume);
    }

    @Override protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        Fluid fluid = BuiltInRegistries.FLUID.getValue(Identifier.parse(input.getStringOr("NutrientFluid", "minecraft:empty")));
        nutrient.setRaw(fluid == null ? Fluids.EMPTY : fluid, input.getIntOr("NutrientAmount", 0));
        state = GreenhouseState.byOrdinal(input.getIntOr("GreenhouseState", 0));
        volume = Math.max(0, input.getIntOr("Volume", 0));
        // Interior is rebuilt by the next revalidation tick; force it to run promptly after load.
        revalidateTimer = 1;
        upkeepTimer = 2;
    }
}
