package za.co.neroland.neroagriculture.lifesupport;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.neroagriculture.registry.ModBlockEntities;

/** NF bioreactor block (registered as the OXYGEN_PLANT id); item/fluid I/O via hoppers/pipes and side config. */
public final class BioreactorBlock extends BaseEntityBlock {
    public static final MapCodec<BioreactorBlock> CODEC = simpleCodec(BioreactorBlock::new);

    public BioreactorBlock(Properties properties) {
        super(properties);
    }

    @Override protected MapCodec<BioreactorBlock> codec() { return CODEC; }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new BioreactorBlockEntity(pos, state); }

    @Override
    public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state,
            @Nullable BlockEntity blockEntity, ItemStack tool) {
        if (blockEntity instanceof BioreactorBlockEntity machine) Containers.dropContents(level, pos, machine);
        super.playerDestroy(level, player, pos, state, blockEntity, tool);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, ModBlockEntities.BIOREACTOR.get(),
                BioreactorBlockEntity::tick);
    }
}
