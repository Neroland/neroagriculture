package za.co.neroland.neroagriculture.fluid;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;

import za.co.neroland.neroagriculture.registry.ModBlocks;
import za.co.neroland.neroagriculture.registry.ModItems;

public abstract class NutrientFluid extends FlowingFluid {
    @Override public Fluid getFlowing() { return ModFluids.FLOWING_NUTRIENT.get(); }
    @Override public Fluid getSource() { return ModFluids.NUTRIENT.get(); }
    @Override public Item getBucket() { return ModItems.NUTRIENT_BUCKET.get(); }
    @Override public boolean isSame(Fluid fluid) { return fluid == getSource() || fluid == getFlowing(); }
    @Override protected boolean canConvertToSource(ServerLevel level) { return false; }
    @Override protected void beforeDestroyingBlock(LevelAccessor level, BlockPos pos, BlockState state) { }
    @Override protected int getSlopeFindDistance(LevelReader level) { return 2; }
    @Override protected int getDropOff(LevelReader level) { return 2; }
    @Override public int getTickDelay(LevelReader level) { return 10; }
    @Override protected float getExplosionResistance() { return 100.0F; }
    @Override public boolean canBeReplacedWith(FluidState state, BlockGetter level, BlockPos pos, Fluid fluid, Direction direction) { return direction == Direction.DOWN && !isSame(fluid); }
    @Override protected BlockState createLegacyBlock(FluidState state) { return ModBlocks.NUTRIENT.get().defaultBlockState().setValue(LiquidBlock.LEVEL, getLegacyLevel(state)); }
    public static final class Source extends NutrientFluid { @Override public int getAmount(FluidState state) { return 8; } @Override public boolean isSource(FluidState state) { return true; } }
    public static final class Flowing extends NutrientFluid {
        @Override protected void createFluidStateDefinition(StateDefinition.Builder<Fluid, FluidState> builder) { super.createFluidStateDefinition(builder); builder.add(LEVEL); }
        @Override public int getAmount(FluidState state) { return state.getValue(LEVEL); }
        @Override public boolean isSource(FluidState state) { return false; }
    }
}
