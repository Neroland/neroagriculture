package za.co.neroland.neroagriculture.content;

import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.state.BlockState;

import za.co.neroland.neroagriculture.crop.SpeciesCropBlockEntity;
import za.co.neroland.neroagriculture.food.FoodCatalog;
import za.co.neroland.neroagriculture.food.FoodDefinition;
import za.co.neroland.neroagriculture.registry.ModBlocks;
import za.co.neroland.neroagriculture.registry.ModDataComponents;
import za.co.neroland.nerolandcore.progression.ProgressionGates;

/** Places the generic food/alien crop only after server-side species/kind/gate/bed validation. */
public final class SpeciesSeedItem extends MaterialVariantItem {
    private final FoodDefinition.Kind kind;

    public SpeciesSeedItem(FoodDefinition.Kind kind, Properties properties) {
        super(properties);
        this.kind = kind;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getClickedFace() != Direction.UP) return InteractionResult.PASS;
        var level = context.getLevel();
        var player = context.getPlayer();
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.FAIL;
        ItemStack stack = context.getItemInHand();
        SpeciesVariant variant = stack.get(ModDataComponents.SPECIES_VARIANT.get());
        if (variant == null) return fail(serverPlayer, "warning.neroagriculture.invalid_seed");

        FoodDefinition definition = FoodCatalog.lookup(serverPlayer.level().getServer(), variant.species()).orElse(null);
        if (definition == null || definition.kind() != kind) return fail(serverPlayer, "warning.neroagriculture.unknown_material");
        var bedTier = ModBlocks.growBedTier(level.getBlockState(context.getClickedPos()).getBlock());
        if (bedTier == null || bedTier.ordinal() < definition.tier().ordinal()) {
            return fail(serverPlayer, "warning.neroagriculture.wrong_bed");
        }
        if (definition.gate() != null && !ProgressionGates.isOpen(serverPlayer, definition.gate())) {
            return fail(serverPlayer, "warning.neroagriculture.gate_closed");
        }
        var cropPos = context.getClickedPos().above();
        if (!level.getBlockState(cropPos).canBeReplaced()) return InteractionResult.FAIL;
        BlockState cropState = (kind == FoodDefinition.Kind.ALIEN
                ? ModBlocks.ALIEN_CROP.get() : ModBlocks.ENGINEERED_FOOD_CROP.get()).defaultBlockState();
        if (!level.setBlock(cropPos, cropState, 3)) return InteractionResult.FAIL;
        if (!(level.getBlockEntity(cropPos) instanceof SpeciesCropBlockEntity crop)) {
            level.removeBlock(cropPos, false);
            return InteractionResult.FAIL;
        }
        crop.setSpecies(definition.id(), stack.getOrDefault(ModDataComponents.HARVEST_COUNT.get(), 0));
        crop.setGenetics(stack.get(ModDataComponents.GENETICS.get()));
        if (!serverPlayer.getAbilities().instabuild) stack.shrink(1);
        return InteractionResult.SUCCESS;
    }

    private static InteractionResult fail(ServerPlayer player, String key) {
        player.sendSystemMessage(Component.translatable(key));
        return InteractionResult.FAIL;
    }
}
