package za.co.neroland.neroagriculture.crop;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Generic crop shell. Deliberately has no block-entity ticker; Stage 4 growth stays random/scheduled. */
public final class ResourceCropBlock extends BaseEntityBlock {
    public static final MapCodec<ResourceCropBlock> CODEC = simpleCodec(ResourceCropBlock::new);
    public ResourceCropBlock(Properties properties) { super(properties); }
    @Override protected MapCodec<ResourceCropBlock> codec() { return CODEC; }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new ResourceCropBlockEntity(pos, state); }
}
