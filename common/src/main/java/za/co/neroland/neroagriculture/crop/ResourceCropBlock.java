package za.co.neroland.neroagriculture.crop;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
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

import za.co.neroland.neroagriculture.balance.TierBalance;
import za.co.neroland.neroagriculture.catalog.MaterialCatalog;
import za.co.neroland.neroagriculture.config.AgricultureConfig;
import za.co.neroland.neroagriculture.content.EssenceFamily;
import za.co.neroland.neroagriculture.content.MaterialVariant;
import za.co.neroland.neroagriculture.registry.ModBlocks;
import za.co.neroland.neroagriculture.registry.ModDataComponents;
import za.co.neroland.neroagriculture.registry.ModItems;
import za.co.neroland.nerolandcore.progression.ProgressionGates;

/** Generic crop shell. Deliberately has no block-entity ticker; Stage 4 growth stays random/scheduled. */
public final class ResourceCropBlock extends BaseEntityBlock {
    public static final MapCodec<ResourceCropBlock> CODEC = simpleCodec(ResourceCropBlock::new);
    public static final IntegerProperty AGE = BlockStateProperties.AGE_7;
    public static final int MAX_AGE = 7;

    @SuppressWarnings("this-escape")
    public ResourceCropBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(AGE, 0));
    }
    @Override protected MapCodec<ResourceCropBlock> codec() { return CODEC; }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new ResourceCropBlockEntity(pos, state); }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(AGE); }
    @Override protected boolean isRandomlyTicking(BlockState state) { return state.getValue(AGE) < MAX_AGE; }
    @Override protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return ModBlocks.growBedTier(level.getBlockState(pos.below()).getBlock()) != null;
    }
    @Override protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks,
            BlockPos pos, Direction direction, BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        return state.canSurvive(level, pos) ? super.updateShape(state, level, ticks, pos, direction,
                neighbourPos, neighbourState, random) : Blocks.AIR.defaultBlockState();
    }

    @Override protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(AGE) >= MAX_AGE || random.nextDouble() >= Math.min(1.0,
                0.25 * AgricultureConfig.GROWTH_MULTIPLIER.get())) return;
        if (!(level.getBlockEntity(pos) instanceof ResourceCropBlockEntity crop)) return;
        GrowthRules.BlockedReason reason = growthReason(level, pos, crop, null);
        if (reason != GrowthRules.BlockedReason.NONE) return;
        EssenceFamily resolvedTier = MaterialCatalog.forServer(level.getServer()).lookup(crop.variant().material())
                .material().map(material -> material.definition().tier()).orElse(crop.variant().family());
        if (resolvedTier != EssenceFamily.TERRAN
                && (!(level.getBlockEntity(pos.below()) instanceof GrowBedBlockEntity bed)
                        || !bed.consumeGrowthResources())) return;
        int step = za.co.neroland.neroagriculture.genetics.GeneticEffects.growthStep(
                za.co.neroland.neroagriculture.fertiliser.Fertilisers.speedStep(level, pos.below()), crop.genetics());
        level.setBlock(pos, state.setValue(AGE, Math.min(MAX_AGE, state.getValue(AGE) + step)), 3);
    }

    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
            Player player, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer serverPlayer)
                || !(level.getBlockEntity(pos) instanceof ResourceCropBlockEntity crop)) return InteractionResult.FAIL;
        var lookup = MaterialCatalog.forServer(serverPlayer.level().getServer()).lookup(crop.variant().material());
        if (player.isShiftKeyDown()) {
            GrowthRules.BlockedReason reason = growthReason((ServerLevel) level, pos, crop, serverPlayer);
            var definition = lookup.material().map(material -> material.definition()).orElse(null);
            double multiplier = AgricultureConfig.YIELD_MULTIPLIER.get();
            int harvests = crop.variant().harvestCount();
            EssenceFamily requiredBed = definition == null ? crop.variant().family() : definition.tier();
            int cap = tierCap(requiredBed);
            int yield = definition == null ? 0
                    : YieldCurve.scaledCapped(definition.yield(), harvests, multiplier, cap);
            int nextYield = definition == null ? 0
                    : YieldCurve.nextCapped(definition.yield(), harvests, multiplier, cap);
            int maxYield = definition == null ? 0 : YieldCurve.maxCapped(definition.yield(), multiplier, cap);
            EssenceFamily actualBed = ModBlocks.growBedTier(level.getBlockState(pos.below()).getBlock());
            String gate = definition == null || definition.gate() == null ? "none"
                    : definition.gate() + ":" + (ProgressionGates.isOpen(serverPlayer, definition.gate())
                            ? "open" : "closed");
            player.sendSystemMessage(Component.literal("Material=" + crop.variant().material() + " tier="
                    + requiredBed + " gate=" + gate + " requiredBed=" + requiredBed + " bed=" + actualBed
                    + " age=" + state.getValue(AGE) + " harvests=" + harvests
                    + " yield=" + yield + " next=" + nextYield + " max=" + maxYield + " cap=" + cap
                    + " blocked=" + reason));
            return InteractionResult.SUCCESS;
        }
        if (state.getValue(AGE) < MAX_AGE) return InteractionResult.PASS;
        if (!lookup.permitsGrowth()) return fail(serverPlayer, lookup.warningKey());
        var definition = lookup.material().orElseThrow().definition();
        if (definition.gate() != null && !ProgressionGates.isOpen(serverPlayer, definition.gate())) {
            return fail(serverPlayer, "warning.neroagriculture.gate_closed");
        }
        int amount = YieldCurve.scaledCapped(definition.yield(), crop.variant().harvestCount(),
                AgricultureConfig.YIELD_MULTIPLIER.get(), tierCap(definition.tier()))
                + za.co.neroland.neroagriculture.fertiliser.Fertilisers.yieldBonus(level, pos.below())
                + za.co.neroland.neroagriculture.genetics.GeneticEffects.yieldBonus(crop.genetics());
        crop.setVariant(new CropVariantState(CropVariantState.CURRENT_FORMAT, definition.id(), definition.tier(),
                crop.variant().harvestCount()).harvested());
        level.setBlock(pos, state.setValue(AGE, 0), 3);
        giveEssence(serverPlayer, pos, definition.id(), definition.tier(), amount);
        return InteractionResult.SUCCESS;
    }

    private static GrowthRules.BlockedReason growthReason(ServerLevel level, BlockPos pos,
            ResourceCropBlockEntity crop, @Nullable ServerPlayer explicitPlayer) {
        var lookup = MaterialCatalog.forServer(level.getServer()).lookup(crop.variant().material());
        EssenceFamily materialTier = lookup.material().map(material -> material.definition().tier())
                .orElse(crop.variant().family());
        var definition = lookup.material().map(material -> material.definition()).orElse(null);
        EssenceFamily bedTier = ModBlocks.growBedTier(level.getBlockState(pos.below()).getBlock());
        ServerPlayer player = explicitPlayer;
        if (player == null) {
            Player nearest = level.getNearestPlayer(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    32.0, false);
            if (nearest instanceof ServerPlayer serverPlayer) player = serverPlayer;
        }
        boolean gate = definition == null || definition.gate() == null
                || player != null && ProgressionGates.isOpen(player, definition.gate());
        boolean dimension = definition == null || definition.worldRestriction() == null
                || definition.worldRestriction().dimension().equals(level.dimension().identifier());
        boolean power = materialTier == EssenceFamily.TERRAN;
        boolean nutrient = materialTier == EssenceFamily.TERRAN;
        if (level.getBlockEntity(pos.below()) instanceof GrowBedBlockEntity bed) {
            int energyCost = AgricultureConfig.GROW_BED_ENERGY_COST.get();
            int nutrientCost = AgricultureConfig.GROW_BED_NUTRIENT_COST.get();
            power = energyCost == 0 || bed.getEnergy().extract(energyCost, true) >= energyCost;
            nutrient = nutrientCost == 0 || bed.getFluid().getFluid() == za.co.neroland.neroagriculture.fluid.ModFluids.NUTRIENT.get()
                    && bed.getFluid().drain(nutrientCost, true) >= nutrientCost;
        }
        var climate = za.co.neroland.neroagriculture.environment.CropClimate.evaluate(
                za.co.neroland.neroagriculture.environment.GrowthEnvironment.worldProfile(level, pos),
                za.co.neroland.neroagriculture.greenhouse.GreenhouseIndex.sealedAt(level, pos),
                materialTier.ordinal(),
                za.co.neroland.neroagriculture.environment.CropClimate.thresholdOrdinal(AgricultureConfig.CONTROLLED_TIER.get()),
                crop.genetics().hardiness(), AgricultureConfig.GENETICS_HARDINESS_RELAX.get());
        return GrowthRules.evaluate(new GrowthRules.Conditions(lookup.status(), materialTier, bedTier, gate,
                level.getRawBrightness(pos, 0) >= 9, dimension, climate, power, nutrient));
    }

    private static void giveEssence(ServerPlayer player, BlockPos pos, net.minecraft.resources.Identifier material,
            EssenceFamily family, int amount) {
        int remaining = amount;
        while (remaining > 0) {
            ItemStack stack = new ItemStack(ModItems.MATERIAL_ESSENCE.get(), Math.min(64, remaining));
            stack.set(ModDataComponents.MATERIAL_VARIANT.get(), MaterialVariant.of(material, family));
            remaining -= stack.getCount();
            if (!player.getInventory().add(stack)) popResource(player.level(), pos, stack);
        }
    }

    @Override public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state,
            @Nullable BlockEntity blockEntity, ItemStack destroyedWith) {
        super.playerDestroy(level, player, pos, state, blockEntity, destroyedWith);
        if (!level.isClientSide() && !player.getAbilities().instabuild && blockEntity instanceof ResourceCropBlockEntity crop) {
            ItemStack seed = new ItemStack(ModItems.RESOURCE_SEED.get());
            var lookup = level.getServer() == null ? MaterialCatalog.current().lookup(crop.variant().material())
                    : MaterialCatalog.forServer(level.getServer()).lookup(crop.variant().material());
            EssenceFamily family = lookup.material().map(material -> material.definition().tier()).orElse(crop.variant().family());
            seed.set(ModDataComponents.MATERIAL_VARIANT.get(), MaterialVariant.of(crop.variant().material(), family));
            seed.set(ModDataComponents.HARVEST_COUNT.get(), crop.variant().harvestCount());
            if (!crop.genetics().isEmpty()) seed.set(ModDataComponents.GENETICS.get(), crop.genetics());
            popResource(level, pos, seed);
        }
    }

    private static int tierCap(EssenceFamily tier) {
        return TierBalance.yieldCap(tier, AgricultureConfig.YIELD_TIER_CAP_BASE.get(),
                AgricultureConfig.YIELD_TIER_CAP_STEP.get());
    }

    private static InteractionResult fail(ServerPlayer player, String key) {
        player.sendSystemMessage(Component.translatable(key));
        return InteractionResult.FAIL;
    }
}
