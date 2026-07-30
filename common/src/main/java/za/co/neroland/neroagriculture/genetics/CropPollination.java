package za.co.neroland.neroagriculture.genetics;

import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import za.co.neroland.neroagriculture.config.AgricultureConfig;
import za.co.neroland.neroagriculture.crop.SpeciesCropBlock;
import za.co.neroland.neroagriculture.crop.SpeciesCropBlockEntity;
import za.co.neroland.neroagriculture.content.SpeciesVariant;
import za.co.neroland.neroagriculture.food.FoodCatalog;
import za.co.neroland.neroagriculture.food.FoodDefinition;
import za.co.neroland.neroagriculture.registry.ModDataComponents;
import za.co.neroland.neroagriculture.registry.ModItems;
import za.co.neroland.nerolandcore.progression.ProgressionGates;

/** Server-authoritative adjacency cross-pollination: a mature crop pair with a breeding match drops a child seed. */
public final class CropPollination {
    private CropPollination() { }

    /** Attempt one pollination at a mature crop with the given percent chance; returns true if a seed dropped. */
    public static boolean attempt(ServerLevel level, BlockPos pos, int chancePercent) {
        if (!mature(level, pos) || !(level.getBlockEntity(pos) instanceof SpeciesCropBlockEntity crop)) return false;
        long seed = pos.asLong() ^ (level.getGameTime() * 0x2545F4914F6CDD1DL);
        if (!Pollination.roll(seed, chancePercent)) return false;
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos neighbourPos = pos.relative(direction);
            if (!mature(level, neighbourPos) || !(level.getBlockEntity(neighbourPos) instanceof SpeciesCropBlockEntity neighbour)) continue;
            Optional<BreedingCatalog.Definition> match = BreedingCatalog.match(level.getServer(), crop.species(), neighbour.species());
            if (match.isEmpty() || !researchOk(level, pos, match.get())) continue;
            FoodDefinition childDefinition = FoodCatalog.lookup(level.getServer(), match.get().child()).orElse(null);
            // Natural alien strains are found, never bred; refuse them as a breeding child even if a datapack asks.
            if (childDefinition == null || !childDefinition.synthesizable()) continue;
            boolean mutate = Pollination.roll(seed * 31 + 7, AgricultureConfig.POLLINATION_MUTATION_PERCENT.get());
            Genetics childGenetics = Pollination.childGenetics(crop.genetics(), neighbour.genetics(), mutate, seed);
            ItemStack childSeed = new ItemStack(childDefinition.kind() == FoodDefinition.Kind.ALIEN
                    ? ModItems.ALIEN_SEED.get() : ModItems.FOOD_SEED.get());
            childSeed.set(ModDataComponents.SPECIES_VARIANT.get(), SpeciesVariant.of(childDefinition.id()));
            if (!childGenetics.isEmpty()) childSeed.set(ModDataComponents.GENETICS.get(), childGenetics);
            Block.popResource(level, pos.above(), childSeed);
            return true;
        }
        return false;
    }

    private static boolean mature(ServerLevel level, BlockPos pos) {
        var state = level.getBlockState(pos);
        return state.getBlock() instanceof SpeciesCropBlock && state.getValue(SpeciesCropBlock.AGE) >= SpeciesCropBlock.MAX_AGE;
    }

    private static boolean researchOk(ServerLevel level, BlockPos pos, BreedingCatalog.Definition definition) {
        if (definition.research() == null) return true;
        // Owner-first: the research gate is checked against the grow bed's recorded owner where one
        // exists; the nearest player is only a fallback for ownerless beds (owner tracking opted out).
        java.util.UUID owner = level.getBlockEntity(pos.below())
                instanceof za.co.neroland.neroagriculture.crop.GrowBedBlockEntity bed ? bed.automationOwner() : null;
        ServerPlayer player = za.co.neroland.neroagriculture.automation.AutomationOwner.gatePlayer(level, pos, owner, 48.0);
        return player != null && ProgressionGates.isOpen(player, definition.research());
    }
}
