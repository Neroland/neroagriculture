package za.co.neroland.neroagriculture.tower;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerolandcore.registry.BlockCodecs;
import za.co.neroland.neroagriculture.progression.AgricultureMilestones;
import za.co.neroland.neroagriculture.registry.ModBlockEntities;

/** Crop tower controller; right-click for status. Contents (stored items + slot seeds) drop on break. */
public final class CropTowerControllerBlock extends BaseEntityBlock {
    public static final MapCodec<CropTowerControllerBlock> CODEC = BlockCodecs.simple(CropTowerControllerBlock::new);

    public CropTowerControllerBlock(Properties properties) {
        super(properties);
    }

    protected MapCodec<CropTowerControllerBlock> codec() { return CODEC; }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new CropTowerControllerBlockEntity(pos, state); }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state,
            @Nullable net.minecraft.world.entity.LivingEntity placer, net.minecraft.world.item.ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide() && placer instanceof Player player
                && level.getBlockEntity(pos) instanceof CropTowerControllerBlockEntity tower) {
            tower.setOwner(player.getUUID());
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof CropTowerControllerBlockEntity tower) {
            if (za.co.neroland.neroagriculture.automation.AutomationOwner.canClaim(tower.automationOwner())) tower.setOwner(player.getUUID());
            if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer && tower.formed()) {
                AgricultureMilestones.record(serverPlayer, AgricultureMilestones.TOWER_GROWER);
            }
            player.openMenu(tower);
        }
        return InteractionResult.SUCCESS;
    }

    //? if >=1.21.2 {
    @Override
    protected InteractionResult useItemOn(net.minecraft.world.item.ItemStack stack, BlockState state, Level level,
            BlockPos pos, Player player, net.minecraft.world.InteractionHand hand, BlockHitResult hit) {
    //?} else {
    /*@Override
    protected net.minecraft.world.ItemInteractionResult useItemOn(net.minecraft.world.item.ItemStack stack, BlockState state, Level level,
            BlockPos pos, Player player, net.minecraft.world.InteractionHand hand, BlockHitResult hit) {
        return za.co.neroland.neroagriculture.platform.InteractionCompat.itemResult(
                useItemOnLegacy(stack, state, level, pos, player, hand, hit));
    }

    // 1.21.1 splits item-use results into ItemInteractionResult; the shared logic stays in InteractionResult.
    private net.minecraft.world.InteractionResult useItemOnLegacy(net.minecraft.world.item.ItemStack stack, BlockState state, Level level,
            BlockPos pos, Player player, net.minecraft.world.InteractionHand hand, BlockHitResult hit) {
    *///?}
        return useWithoutItem(state, level, pos, player, hit);
    }

    // Contents (stored items + slot seeds) drop from CropTowerControllerBlockEntity#preRemoveSideEffects
    // (covers creative breaks and explosions too), so no playerDestroy override is needed here.

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, ModBlockEntities.CROP_TOWER_CONTROLLER.get(),
                CropTowerControllerBlockEntity::tick);
    }

    //? if <1.21.5 {
    /*// Before 1.21.5 removal side effects run from the block (block entities get preRemoveSideEffects later).
    @Override
    protected void onRemove(BlockState state, net.minecraft.world.level.Level level, BlockPos pos, BlockState newState,
            boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof za.co.neroland.neroagriculture.tower.CropTowerControllerBlockEntity be) {
            be.preRemoveSideEffects(pos, state);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
    *///?}
}
