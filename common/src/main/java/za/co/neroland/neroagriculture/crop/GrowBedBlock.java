package za.co.neroland.neroagriculture.crop;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import za.co.neroland.neroagriculture.content.FragmentTier;

/** Powered Forgite–Voidite bed. Territe remains a passive plain block. */
public final class GrowBedBlock extends BaseEntityBlock {
    public static final MapCodec<GrowBedBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            FragmentTier.CODEC.fieldOf("tier").forGetter(GrowBedBlock::tier),
            propertiesCodec()).apply(instance, GrowBedBlock::new));
    private final FragmentTier tier;

    public GrowBedBlock(FragmentTier tier, Properties properties) {
        super(properties);
        if (tier == FragmentTier.TERRITE) throw new IllegalArgumentException("Territe bed is passive");
        this.tier = tier;
    }
    public FragmentTier tier() { return tier; }
    @Override protected MapCodec<GrowBedBlock> codec() { return CODEC; }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
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
            player.openMenu(bed);
        }
        return net.minecraft.world.InteractionResult.SUCCESS;
    }

    @Override
    protected net.minecraft.world.InteractionResult useItemOn(net.minecraft.world.item.ItemStack stack,
            BlockState state, net.minecraft.world.level.Level level, BlockPos pos,
            net.minecraft.world.entity.player.Player player, net.minecraft.world.InteractionHand hand,
            net.minecraft.world.phys.BlockHitResult hit) {
        // Seeds keep their hand-planting behaviour (the seed item's useOn plants above the bed).
        if (stack.is(za.co.neroland.neroagriculture.registry.ModItems.RESOURCE_SEED.get())
                || stack.is(za.co.neroland.neroagriculture.registry.ModItems.FOOD_SEED.get())
                || stack.is(za.co.neroland.neroagriculture.registry.ModItems.ALIEN_SEED.get())) {
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
}
