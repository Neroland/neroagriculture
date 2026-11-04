package za.co.neroland.neroagriculture.crop;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import za.co.neroland.nerolandcore.registry.BlockCodecs;
import za.co.neroland.neroagriculture.content.FragmentTier;

/** Grow bed with a shared inventory; Territe crops grow without power or nutrient. */
public final class GrowBedBlock extends BaseEntityBlock {
    public static final MapCodec<GrowBedBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            FragmentTier.CODEC.fieldOf("tier").forGetter(GrowBedBlock::tier),
            BlockCodecs.properties()).apply(instance, GrowBedBlock::new));
    private final FragmentTier tier;

    public GrowBedBlock(FragmentTier tier, Properties properties) {
        super(properties);
        this.tier = tier;
    }
    public FragmentTier tier() { return tier; }
    protected MapCodec<GrowBedBlock> codec() { return CODEC; }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override protected net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState state,
            net.minecraft.world.level.BlockGetter level, BlockPos pos,
            net.minecraft.world.phys.shapes.CollisionContext context) {
        return box(0, 0, 0, 16, 10, 16);
    }
    @Override protected net.minecraft.world.phys.shapes.VoxelShape getCollisionShape(BlockState state,
            net.minecraft.world.level.BlockGetter level, BlockPos pos,
            net.minecraft.world.phys.shapes.CollisionContext context) {
        return net.minecraft.world.phys.shapes.Shapes.block();
    }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new GrowBedBlockEntity(pos, state); }

    @Override
    public void setPlacedBy(net.minecraft.world.level.Level level, BlockPos pos, BlockState state,
            @org.jetbrains.annotations.Nullable net.minecraft.world.entity.LivingEntity placer,
            net.minecraft.world.item.ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide() && placer instanceof net.minecraft.world.entity.player.Player player
                && level.getBlockEntity(pos) instanceof GrowBedBlockEntity bed) {
            bed.setOwner(player.getUUID());
        }
    }

    @Override
    protected net.minecraft.world.InteractionResult useWithoutItem(BlockState state,
            net.minecraft.world.level.Level level, BlockPos pos, net.minecraft.world.entity.player.Player player,
            net.minecraft.world.phys.BlockHitResult hit) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof GrowBedBlockEntity bed) {
            if (za.co.neroland.neroagriculture.automation.AutomationOwner.canClaim(bed.automationOwner())) bed.setOwner(player.getUUID());
            player.openMenu(bed);
        }
        return net.minecraft.world.InteractionResult.SUCCESS;
    }

    //? if >=1.21.2 {
    @Override
    protected net.minecraft.world.InteractionResult useItemOn(net.minecraft.world.item.ItemStack stack,
            BlockState state, net.minecraft.world.level.Level level, BlockPos pos,
            net.minecraft.world.entity.player.Player player, net.minecraft.world.InteractionHand hand,
            net.minecraft.world.phys.BlockHitResult hit) {
    //?} else {
    /*@Override
    protected net.minecraft.world.ItemInteractionResult useItemOn(net.minecraft.world.item.ItemStack stack,
            BlockState state, net.minecraft.world.level.Level level, BlockPos pos,
            net.minecraft.world.entity.player.Player player, net.minecraft.world.InteractionHand hand,
            net.minecraft.world.phys.BlockHitResult hit) {
        return za.co.neroland.neroagriculture.platform.InteractionCompat.itemResult(
                useItemOnLegacy(stack, state, level, pos, player, hand, hit));
    }

    // 1.21.1 splits item-use results into ItemInteractionResult; the shared logic stays in InteractionResult.
    private net.minecraft.world.InteractionResult useItemOnLegacy(net.minecraft.world.item.ItemStack stack,
            BlockState state, net.minecraft.world.level.Level level, BlockPos pos,
            net.minecraft.world.entity.player.Player player, net.minecraft.world.InteractionHand hand,
            net.minecraft.world.phys.BlockHitResult hit) {
    *///?}
        boolean speed = stack.is(za.co.neroland.neroagriculture.registry.ModItems.SPEED_FERTILISER.get());
        boolean yield = stack.is(za.co.neroland.neroagriculture.registry.ModItems.YIELD_FERTILISER.get());
        if (speed || yield) {
            if (level.isClientSide()) return net.minecraft.world.InteractionResult.SUCCESS;
            if (!(level.getBlockEntity(pos) instanceof GrowBedBlockEntity bed)) return net.minecraft.world.InteractionResult.FAIL;
            var type = speed ? za.co.neroland.neroagriculture.fertiliser.FertiliserType.SPEED
                    : za.co.neroland.neroagriculture.fertiliser.FertiliserType.YIELD;
            if (!za.co.neroland.neroagriculture.fertiliser.Fertilisers.applyOne(bed, type, level.getGameTime(),
                    za.co.neroland.neroagriculture.config.AgricultureConfig.FERTILISER_DURATION_TICKS.get(),
                    za.co.neroland.neroagriculture.config.AgricultureConfig.FERTILISER_MAX_DOSE.get())) {
                return net.minecraft.world.InteractionResult.FAIL;
            }
            if (!player.getAbilities().instabuild) stack.shrink(1);
            if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                serverPlayer.sendSystemMessage(net.minecraft.network.chat.Component.translatable(speed
                        ? "message.neroagriculture.fertiliser.speed_applied"
                        : "message.neroagriculture.fertiliser.yield_applied"));
            }
            return net.minecraft.world.InteractionResult.SUCCESS;
        }
        // Seeds keep their hand-planting behaviour (the seed item's useOn plants above the bed).
        boolean seed = stack.is(za.co.neroland.neroagriculture.registry.ModItems.RESOURCE_SEED.get())
                || stack.is(za.co.neroland.neroagriculture.registry.ModItems.FOOD_SEED.get())
                || stack.is(za.co.neroland.neroagriculture.registry.ModItems.ALIEN_SEED.get());
        if (GrowBedInteraction.plantsFromFace(seed, hit.getDirection())) {
            return net.minecraft.world.InteractionResult.PASS;
        }
        return useWithoutItem(state, level, pos, player, hit);
    }

    @org.jetbrains.annotations.Nullable
    @Override
    public <T extends BlockEntity> net.minecraft.world.level.block.entity.BlockEntityTicker<T> getTicker(
            net.minecraft.world.level.Level level, BlockState state,
            net.minecraft.world.level.block.entity.BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type,
                za.co.neroland.neroagriculture.registry.ModBlockEntities.GROW_BED.get(), GrowBedBlockEntity::tick);
    }

    //? if <1.21.5 {
    /*// Before 1.21.5 removal side effects run from the block (block entities get preRemoveSideEffects later).
    @Override
    protected void onRemove(BlockState state, net.minecraft.world.level.Level level, BlockPos pos, BlockState newState,
            boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof za.co.neroland.neroagriculture.crop.GrowBedBlockEntity be) {
            be.preRemoveSideEffects(pos, state);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
    *///?}
}
