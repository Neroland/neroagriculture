package za.co.neroland.neroagriculture.crop;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.neroagriculture.config.AgricultureConfig;
import za.co.neroland.neroagriculture.content.FragmentTier;
import za.co.neroland.neroagriculture.content.SpeciesVariant;
import za.co.neroland.neroagriculture.food.FoodCatalog;
import za.co.neroland.neroagriculture.food.FoodDefinition;
import za.co.neroland.neroagriculture.registry.ModBlocks;
import za.co.neroland.neroagriculture.registry.ModDataComponents;
import za.co.neroland.neroagriculture.registry.ModItems;

/** Generic engineered-food / alien crop shell. Non-ticking; growth stays random/scheduled and fail-closed. */
public final class SpeciesCropBlock extends BaseEntityBlock {
    public static final MapCodec<SpeciesCropBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            FoodDefinition.Kind.CODEC.fieldOf("kind").forGetter(SpeciesCropBlock::kind),
            propertiesCodec()).apply(instance, SpeciesCropBlock::new));
    public static final IntegerProperty AGE = BlockStateProperties.AGE_7;
    public static final int MAX_AGE = 7;
    private final FoodDefinition.Kind kind;

    @SuppressWarnings("this-escape")
    public SpeciesCropBlock(FoodDefinition.Kind kind, Properties properties) {
        super(properties);
        this.kind = kind;
        registerDefaultState(stateDefinition.any().setValue(AGE, 0));
    }

    public FoodDefinition.Kind kind() { return kind; }
    @Override protected MapCodec<SpeciesCropBlock> codec() { return CODEC; }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new SpeciesCropBlockEntity(pos, state); }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(AGE); }
    @Override protected boolean isRandomlyTicking(BlockState state) { return true; }
    @Override protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return ModBlocks.growBedTier(level.getBlockState(pos.below()).getBlock()) != null;
    }
    @Override protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks,
            BlockPos pos, Direction direction, BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        return state.canSurvive(level, pos) ? super.updateShape(state, level, ticks, pos, direction,
                neighbourPos, neighbourState, random) : Blocks.AIR.defaultBlockState();
    }

    @Override protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!(level.getBlockEntity(pos) instanceof SpeciesCropBlockEntity crop)) return;
        if (state.getValue(AGE) >= MAX_AGE) {
            za.co.neroland.neroagriculture.genetics.CropPollination.attempt(level, pos,
                    AgricultureConfig.POLLINATION_CHANCE_PERCENT.get());
            return;
        }
        double cycleGrowth = za.co.neroland.neroagriculture.cycle.Cycles.current(level.getServer(),
                level.dimension().identifier(), level.getGameTime()).growth();
        if (random.nextDouble() >= Math.min(1.0, 0.25 * AgricultureConfig.GROWTH_MULTIPLIER.get() * cycleGrowth)) return;
        FoodDefinition definition = FoodCatalog.forServer(level.getServer()).get(crop.species());
        if (definition == null) return;
        FragmentTier tier = definition.tier();
        FragmentTier bedTier = ModBlocks.growBedTier(level.getBlockState(pos.below()).getBlock());
        if (bedTier == null || bedTier.ordinal() < tier.ordinal()) return;
        if (level.getRawBrightness(pos, 0) < 9) return;
        var climate = za.co.neroland.neroagriculture.environment.CropClimate.evaluate(
                za.co.neroland.neroagriculture.environment.GrowthEnvironment.worldProfile(level, pos),
                za.co.neroland.neroagriculture.greenhouse.GreenhouseIndex.sealedAt(level, pos), tier.ordinal(),
                za.co.neroland.neroagriculture.environment.CropClimate.thresholdOrdinal(AgricultureConfig.CONTROLLED_TIER.get()),
                crop.genetics().hardiness(), AgricultureConfig.GENETICS_HARDINESS_RELAX.get());
        if (climate != za.co.neroland.neroagriculture.environment.CropClimate.Result.OK) return;
        if (tier != FragmentTier.TERRITE
                && (!(level.getBlockEntity(pos.below()) instanceof GrowBedBlockEntity bed) || !bed.consumeGrowthResources())) return;
        int step = za.co.neroland.neroagriculture.genetics.GeneticEffects.growthStep(
                za.co.neroland.neroagriculture.fertiliser.Fertilisers.speedStep(level, pos.below()), crop.genetics());
        level.setBlock(pos, state.setValue(AGE, Math.min(MAX_AGE, state.getValue(AGE) + step)), 3);
    }

    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
            Player player, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer serverPlayer)
                || !(level.getBlockEntity(pos) instanceof SpeciesCropBlockEntity crop)) return InteractionResult.FAIL;
        if (state.getValue(AGE) < MAX_AGE) return InteractionResult.PASS;
        FoodDefinition definition = FoodCatalog.forServer(serverPlayer.level().getServer()).get(crop.species());
        if (definition == null) return InteractionResult.FAIL;
        crop.recordHarvest();
        level.setBlock(pos, state.setValue(AGE, 0), 3);
        ItemStack produce = new ItemStack(kind == FoodDefinition.Kind.ALIEN
                ? ModItems.ALIEN_PRODUCE.get() : ModItems.ENGINEERED_FOOD.get());
        produce.set(ModDataComponents.SPECIES_VARIANT.get(), SpeciesVariant.of(definition.id()));
        if (!crop.genetics().isEmpty()) produce.set(ModDataComponents.GENETICS.get(), crop.genetics());
        if (!serverPlayer.getInventory().add(produce)) popResource(level, pos, produce);
        return InteractionResult.SUCCESS;
    }

    @Override public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state,
            @Nullable BlockEntity blockEntity, ItemStack destroyedWith) {
        super.playerDestroy(level, player, pos, state, blockEntity, destroyedWith);
        if (!level.isClientSide() && !player.getAbilities().instabuild && blockEntity instanceof SpeciesCropBlockEntity crop) {
            ItemStack seed = new ItemStack(kind == FoodDefinition.Kind.ALIEN
                    ? ModItems.ALIEN_SEED.get() : ModItems.FOOD_SEED.get());
            seed.set(ModDataComponents.SPECIES_VARIANT.get(), SpeciesVariant.of(crop.species()));
            seed.set(ModDataComponents.HARVEST_COUNT.get(), crop.harvestCount());
            if (!crop.genetics().isEmpty()) seed.set(ModDataComponents.GENETICS.get(), crop.genetics());
            popResource(level, pos, seed);
        }
    }
}
