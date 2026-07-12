package za.co.neroland.neroagriculture.terraforming;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
import za.co.neroland.neroagriculture.registry.ModItems;

/** Terraforming controller: use a Terraforming Seed to start; sneak-right-click (owner/op) to roll back. */
public final class TerraformingControllerBlock extends BaseEntityBlock {
    public static final MapCodec<TerraformingControllerBlock> CODEC = simpleCodec(TerraformingControllerBlock::new);

    public TerraformingControllerBlock(Properties properties) {
        super(properties);
    }

    @Override protected MapCodec<TerraformingControllerBlock> codec() { return CODEC; }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new TerraformingControllerBlockEntity(pos, state); }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hit) {
        if (!stack.is(ModItems.TERRAFORMING_SEED.get())) return InteractionResult.PASS;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (level instanceof ServerLevel serverLevel && player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof TerraformingControllerBlockEntity controller
                && controller.start(serverLevel, serverPlayer, stack)) {
            serverPlayer.sendSystemMessage(Component.literal("[NeroAgriculture] Terraforming project started."));
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (level instanceof ServerLevel serverLevel && player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof TerraformingControllerBlockEntity controller) {
            if (player.isShiftKeyDown()) {
                boolean rolled = controller.rollback(serverLevel, serverPlayer);
                serverPlayer.sendSystemMessage(Component.literal(rolled
                        ? "[NeroAgriculture] Terraforming rolled back." : "[NeroAgriculture] Not authorised to roll back."));
            } else {
                serverPlayer.sendSystemMessage(Component.literal("[NeroAgriculture] terraforming " + controller.status()));
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, ModBlockEntities.TERRAFORMING_CONTROLLER.get(),
                TerraformingControllerBlockEntity::tick);
    }
}
