package za.co.neroland.neroagriculture.lifesupport;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
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

    // Contents drop from BioreactorBlockEntity#preRemoveSideEffects (covers creative breaks and
    // explosions too), so no playerDestroy override is needed here.

    @Override
    protected net.minecraft.world.InteractionResult useItemOn(net.minecraft.world.item.ItemStack stack,
            BlockState state, Level level, BlockPos pos, net.minecraft.world.entity.player.Player player,
            net.minecraft.world.InteractionHand hand, net.minecraft.world.phys.BlockHitResult hit) {
        return useWithoutItem(state, level, pos, player, hit);
    }

    @Override
    protected net.minecraft.world.InteractionResult useWithoutItem(BlockState state, Level level,
            BlockPos pos, net.minecraft.world.entity.player.Player player, net.minecraft.world.phys.BlockHitResult hit) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof BioreactorBlockEntity machine) {
            player.openMenu(machine);
        }
        return net.minecraft.world.InteractionResult.SUCCESS;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, ModBlockEntities.BIOREACTOR.get(),
                BioreactorBlockEntity::tick);
    }
}
