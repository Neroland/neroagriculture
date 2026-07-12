package za.co.neroland.neroagriculture.genetics;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.neroagriculture.registry.ModBlockEntities;
import za.co.neroland.neroagriculture.registry.ModDataComponents;

/** Genetics Station block. Right-click with a seed to analyse (print) its traits; hoppers load the slots. */
public final class GeneticsStationBlock extends BaseEntityBlock {
    public static final MapCodec<GeneticsStationBlock> CODEC = simpleCodec(GeneticsStationBlock::new);

    public GeneticsStationBlock(Properties properties) {
        super(properties);
    }

    @Override protected MapCodec<GeneticsStationBlock> codec() { return CODEC; }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new GeneticsStationBlockEntity(pos, state); }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        Genetics genetics = stack.get(ModDataComponents.GENETICS.get());
        if (genetics == null) return InteractionResult.PASS;
        player.sendSystemMessage(Component.literal("[Genetics] yield=" + genetics.yield() + " speed=" + genetics.speed()
                + " hardiness=" + genetics.hardiness() + " oxygen=" + genetics.oxygenOutput()
                + " potency=" + genetics.foodPotency() + " total=" + genetics.total() + "/15"));
        return InteractionResult.CONSUME;
    }

    @Override
    public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state,
            @Nullable BlockEntity blockEntity, ItemStack tool) {
        if (blockEntity instanceof GeneticsStationBlockEntity machine) Containers.dropContents(level, pos, machine);
        super.playerDestroy(level, player, pos, state, blockEntity, tool);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, ModBlockEntities.GENETICS_STATION.get(),
                GeneticsStationBlockEntity::tick);
    }
}
