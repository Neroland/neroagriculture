package za.co.neroland.neroagriculture.automation;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
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

/** Planter/Harvester block; the mode is read from which registered block this is. */
public final class AreaMachineBlock extends BaseEntityBlock {
    public static final MapCodec<AreaMachineBlock> CODEC = simpleCodec(AreaMachineBlock::new);

    public AreaMachineBlock(Properties properties) {
        super(properties);
    }

    @Override protected MapCodec<AreaMachineBlock> codec() { return CODEC; }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new AreaMachineBlockEntity(pos, state); }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide() && placer instanceof Player player
                && level.getBlockEntity(pos) instanceof AreaMachineBlockEntity machine) {
            machine.setOwner(player.getUUID());
        }
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hit) {
        if (stack.isEmpty()) return InteractionResult.PASS;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (level.getBlockEntity(pos) instanceof AreaMachineBlockEntity machine) {
            ItemStack leftover = machine.insertHeld(stack);
            if (leftover.getCount() != stack.getCount() || !ItemStack.matches(leftover, stack)) {
                if (!player.getAbilities().instabuild) player.setItemInHand(hand, leftover);
                return InteractionResult.CONSUME;
            }
            // Nothing to insert: open the UI rather than letting vanilla place the held block.
            player.openMenu(machine);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
            BlockHitResult hit) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof AreaMachineBlockEntity machine) {
            player.openMenu(machine);
        }
        return InteractionResult.SUCCESS;
    }

    // Contents drop from AreaMachineBlockEntity#preRemoveSideEffects (covers creative breaks and
    // explosions too), so no playerDestroy override is needed here.

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, ModBlockEntities.AREA_MACHINE.get(),
                AreaMachineBlockEntity::tick);
    }
}
