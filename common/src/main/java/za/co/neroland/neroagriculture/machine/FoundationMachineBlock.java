package za.co.neroland.neroagriculture.machine;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.neroagriculture.registry.ModBlockEntities;

/** Common Stage 2 machine shell; later stages layer recipes and menus onto the stable ids. */
public final class FoundationMachineBlock extends BaseEntityBlock {
    public static final MapCodec<FoundationMachineBlock> CODEC = simpleCodec(FoundationMachineBlock::new);

    public FoundationMachineBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<FoundationMachineBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FoundationMachineBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(net.minecraft.world.level.Level level,
            BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, ModBlockEntities.FOUNDATION_MACHINE.get(),
                FoundationMachineBlockEntity::tick);
    }
}
