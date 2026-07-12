package za.co.neroland.neroagriculture.greenhouse;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Greenhouse glazing. A translucent, non-occluding pane that hides the shared faces between adjacent
 * greenhouse glass (like vanilla glass) so a glazed wall reads as one clean surface instead of a grid of
 * internal quads. Registered {@code noOcclusion} so it never culls its neighbours as if it were solid —
 * fixing the "transparent model on a solid block" render artefact.
 */
public final class GreenhouseGlassBlock extends Block {
    public static final MapCodec<GreenhouseGlassBlock> CODEC = simpleCodec(GreenhouseGlassBlock::new);

    public GreenhouseGlassBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected boolean skipRendering(BlockState state, BlockState adjacentState, Direction direction) {
        return adjacentState.is(this) || super.skipRendering(state, adjacentState, direction);
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state) {
        return true;
    }
}
