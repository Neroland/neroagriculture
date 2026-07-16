package za.co.neroland.neroagriculture.biofuel;

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

/** NF Biofuel Converter block; item/fluid/energy I/O via hoppers/pipes and Core side config. */
public final class BiofuelConverterBlock extends BaseEntityBlock {
    public static final MapCodec<BiofuelConverterBlock> CODEC = simpleCodec(BiofuelConverterBlock::new);

    public BiofuelConverterBlock(Properties properties) {
        super(properties);
    }

    @Override protected MapCodec<BiofuelConverterBlock> codec() { return CODEC; }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new BiofuelConverterBlockEntity(pos, state); }

    @Override
    public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state,
            @Nullable BlockEntity blockEntity, ItemStack tool) {
        if (blockEntity instanceof BiofuelConverterBlockEntity machine) Containers.dropContents(level, pos, machine);
        super.playerDestroy(level, player, pos, state, blockEntity, tool);
    }

    @Override
    protected net.minecraft.world.InteractionResult useWithoutItem(BlockState state, Level level,
            BlockPos pos, net.minecraft.world.entity.player.Player player, net.minecraft.world.phys.BlockHitResult hit) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof BiofuelConverterBlockEntity machine) {
            player.openMenu(machine);
        }
        return net.minecraft.world.InteractionResult.SUCCESS;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, ModBlockEntities.BIOFUEL_CONVERTER.get(),
                BiofuelConverterBlockEntity::tick);
    }
}
