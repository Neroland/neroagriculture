package za.co.neroland.neroagriculture.automation;

import java.util.function.Consumer;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.neroagriculture.balance.TierBalance;
import za.co.neroland.neroagriculture.catalog.MaterialCatalog;
import za.co.neroland.neroagriculture.config.AgricultureConfig;
import za.co.neroland.neroagriculture.content.EssenceFamily;
import za.co.neroland.neroagriculture.content.MaterialVariant;
import za.co.neroland.neroagriculture.content.SpeciesVariant;
import za.co.neroland.neroagriculture.crop.CropVariantState;
import za.co.neroland.neroagriculture.crop.ResourceCropBlock;
import za.co.neroland.neroagriculture.crop.ResourceCropBlockEntity;
import za.co.neroland.neroagriculture.crop.SpeciesCropBlock;
import za.co.neroland.neroagriculture.crop.SpeciesCropBlockEntity;
import za.co.neroland.neroagriculture.crop.YieldCurve;
import za.co.neroland.neroagriculture.food.FoodCatalog;
import za.co.neroland.neroagriculture.food.FoodDefinition;
import za.co.neroland.neroagriculture.registry.ModBlocks;
import za.co.neroland.neroagriculture.registry.ModDataComponents;
import za.co.neroland.neroagriculture.registry.ModItems;
import za.co.neroland.nerolandcore.progression.ProgressionGates;

/**
 * Server-authoritative planting and harvesting used by the Planter/Harvester. Reuses the same catalog, gate,
 * bed-tier, and capped-yield rules as manual play, so automation is never a duplication or bypass path.
 */
public final class AreaFarming {
    private AreaFarming() { }

    /** Plant one seed from the stack onto the bed at {@code bedPos}; returns true if a crop was placed. */
    public static boolean plant(ServerLevel level, BlockPos bedPos, ItemStack seed, @Nullable ServerPlayer gatePlayer) {
        if (seed.isEmpty()) return false;
        EssenceFamily bedTier = ModBlocks.growBedTier(level.getBlockState(bedPos).getBlock());
        if (bedTier == null) return false;
        BlockPos cropPos = bedPos.above();
        if (!level.getBlockState(cropPos).canBeReplaced()) return false;
        if (seed.is(ModItems.RESOURCE_SEED.get())) return plantResource(level, cropPos, bedTier, seed, gatePlayer);
        if (seed.is(ModItems.FOOD_SEED.get()) || seed.is(ModItems.ALIEN_SEED.get())) {
            return plantSpecies(level, cropPos, bedTier, seed, gatePlayer);
        }
        return false;
    }

    private static boolean plantResource(ServerLevel level, BlockPos cropPos, EssenceFamily bedTier, ItemStack seed,
            @Nullable ServerPlayer gatePlayer) {
        MaterialVariant variant = seed.get(ModDataComponents.MATERIAL_VARIANT.get());
        if (variant == null) return false;
        var lookup = MaterialCatalog.forServer(level.getServer()).lookup(variant.material());
        if (!lookup.permitsGrowth()) return false;
        var definition = lookup.material().orElseThrow().definition();
        if (bedTier.ordinal() < definition.tier().ordinal()) return false;
        if (definition.gate() != null && (gatePlayer == null || !ProgressionGates.isOpen(gatePlayer, definition.gate()))) return false;
        if (definition.worldRestriction() != null
                && !definition.worldRestriction().dimension().equals(level.dimension().identifier())) return false;
        if (!level.setBlock(cropPos, ModBlocks.RESOURCE_CROP.get().defaultBlockState(), 3)) return false;
        if (!(level.getBlockEntity(cropPos) instanceof ResourceCropBlockEntity crop)) {
            level.removeBlock(cropPos, false);
            return false;
        }
        int harvests = seed.getOrDefault(ModDataComponents.HARVEST_COUNT.get(), 0);
        crop.setVariant(new CropVariantState(CropVariantState.CURRENT_FORMAT, definition.id(), definition.tier(), harvests));
        crop.setGenetics(seed.get(ModDataComponents.GENETICS.get()));
        seed.shrink(1);
        return true;
    }

    private static boolean plantSpecies(ServerLevel level, BlockPos cropPos, EssenceFamily bedTier, ItemStack seed,
            @Nullable ServerPlayer gatePlayer) {
        SpeciesVariant variant = seed.get(ModDataComponents.SPECIES_VARIANT.get());
        if (variant == null) return false;
        FoodDefinition definition = FoodCatalog.lookup(level.getServer(), variant.species()).orElse(null);
        if (definition == null) return false;
        boolean alien = definition.kind() == FoodDefinition.Kind.ALIEN;
        if (seed.is(ModItems.ALIEN_SEED.get()) != alien) return false;
        if (bedTier.ordinal() < definition.tier().ordinal()) return false;
        if (definition.gate() != null && (gatePlayer == null || !ProgressionGates.isOpen(gatePlayer, definition.gate()))) return false;
        BlockState cropState = (alien ? ModBlocks.ALIEN_CROP.get() : ModBlocks.ENGINEERED_FOOD_CROP.get()).defaultBlockState();
        if (!level.setBlock(cropPos, cropState, 3)) return false;
        if (!(level.getBlockEntity(cropPos) instanceof SpeciesCropBlockEntity crop)) {
            level.removeBlock(cropPos, false);
            return false;
        }
        crop.setSpecies(definition.id(), seed.getOrDefault(ModDataComponents.HARVEST_COUNT.get(), 0));
        crop.setGenetics(seed.get(ModDataComponents.GENETICS.get()));
        seed.shrink(1);
        return true;
    }

    /** True when the crop at {@code cropPos} is a mature Nero crop that a harvester could take. */
    public static boolean isMature(ServerLevel level, BlockPos cropPos) {
        BlockState state = level.getBlockState(cropPos);
        if (state.getBlock() instanceof ResourceCropBlock) return state.getValue(ResourceCropBlock.AGE) >= ResourceCropBlock.MAX_AGE;
        if (state.getBlock() instanceof SpeciesCropBlock) return state.getValue(SpeciesCropBlock.AGE) >= SpeciesCropBlock.MAX_AGE;
        return false;
    }

    /** Harvest a mature crop in place (preserving the plant and its history); emits produce to the sink. */
    public static boolean harvest(ServerLevel level, BlockPos cropPos, @Nullable ServerPlayer gatePlayer,
            int yieldBonus, Consumer<ItemStack> sink) {
        BlockState state = level.getBlockState(cropPos);
        if (state.getBlock() instanceof ResourceCropBlock && state.getValue(ResourceCropBlock.AGE) >= ResourceCropBlock.MAX_AGE) {
            return harvestResource(level, cropPos, state, gatePlayer, yieldBonus, sink);
        }
        if (state.getBlock() instanceof SpeciesCropBlock && state.getValue(SpeciesCropBlock.AGE) >= SpeciesCropBlock.MAX_AGE) {
            return harvestSpecies(level, cropPos, state, sink);
        }
        return false;
    }

    private static boolean harvestResource(ServerLevel level, BlockPos cropPos, BlockState state,
            @Nullable ServerPlayer gatePlayer, int yieldBonus, Consumer<ItemStack> sink) {
        if (!(level.getBlockEntity(cropPos) instanceof ResourceCropBlockEntity crop)) return false;
        var lookup = MaterialCatalog.forServer(level.getServer()).lookup(crop.variant().material());
        if (!lookup.permitsGrowth()) return false;
        var definition = lookup.material().orElseThrow().definition();
        if (definition.gate() != null && (gatePlayer == null || !ProgressionGates.isOpen(gatePlayer, definition.gate()))) return false;
        int cap = TierBalance.yieldCap(definition.tier(), AgricultureConfig.YIELD_TIER_CAP_BASE.get(),
                AgricultureConfig.YIELD_TIER_CAP_STEP.get());
        int amount = YieldCurve.scaledCapped(definition.yield(), crop.variant().harvestCount(),
                AgricultureConfig.YIELD_MULTIPLIER.get(), cap) + Math.max(0, yieldBonus)
                + za.co.neroland.neroagriculture.fertiliser.Fertilisers.yieldBonus(level, cropPos.below())
                + za.co.neroland.neroagriculture.genetics.GeneticEffects.yieldBonus(crop.genetics());
        crop.setVariant(new CropVariantState(CropVariantState.CURRENT_FORMAT, definition.id(), definition.tier(),
                crop.variant().harvestCount()).harvested());
        level.setBlock(cropPos, state.setValue(ResourceCropBlock.AGE, 0), 3);
        emitEssence(sink, definition.id(), definition.tier(), amount);
        return true;
    }

    private static boolean harvestSpecies(ServerLevel level, BlockPos cropPos, BlockState state, Consumer<ItemStack> sink) {
        if (!(level.getBlockEntity(cropPos) instanceof SpeciesCropBlockEntity crop)) return false;
        FoodDefinition definition = FoodCatalog.forServer(level.getServer()).get(crop.species());
        if (definition == null) return false;
        boolean alien = definition.kind() == FoodDefinition.Kind.ALIEN;
        crop.recordHarvest();
        level.setBlock(cropPos, state.setValue(SpeciesCropBlock.AGE, 0), 3);
        ItemStack produce = new ItemStack(alien ? ModItems.ALIEN_PRODUCE.get() : ModItems.ENGINEERED_FOOD.get());
        produce.set(ModDataComponents.SPECIES_VARIANT.get(), SpeciesVariant.of(definition.id()));
        if (!crop.genetics().isEmpty()) produce.set(ModDataComponents.GENETICS.get(), crop.genetics());
        sink.accept(produce);
        return true;
    }

    private static void emitEssence(Consumer<ItemStack> sink, Identifier material, EssenceFamily family, int amount) {
        int remaining = amount;
        while (remaining > 0) {
            ItemStack stack = new ItemStack(ModItems.MATERIAL_ESSENCE.get(), Math.min(64, remaining));
            stack.set(ModDataComponents.MATERIAL_VARIANT.get(), MaterialVariant.of(material, family));
            remaining -= stack.getCount();
            sink.accept(stack);
        }
    }
}
