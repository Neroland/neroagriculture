package za.co.neroland.neroagriculture.content;

import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;

import za.co.neroland.neroagriculture.catalog.MaterialCatalog;
import za.co.neroland.neroagriculture.crop.CropVariantState;
import za.co.neroland.neroagriculture.crop.ResourceCropBlockEntity;
import za.co.neroland.neroagriculture.registry.ModBlocks;
import za.co.neroland.neroagriculture.registry.ModDataComponents;
import za.co.neroland.nerolandcore.progression.ProgressionGates;

/** Places the one generic crop only after server-side catalog/gate/bed/dimension validation. */
public final class ResourceSeedItem extends MaterialVariantItem {
    public ResourceSeedItem(Properties properties) { super(properties); }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getClickedFace() != Direction.UP) return InteractionResult.PASS;
        var level = context.getLevel();
        var player = context.getPlayer();
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.FAIL;
        ItemStack stack = context.getItemInHand();
        MaterialVariant variant = stack.get(ModDataComponents.MATERIAL_VARIANT.get());
        if (variant == null) return fail(serverPlayer, "warning.neroagriculture.invalid_seed");

        var lookup = MaterialCatalog.forServer(serverPlayer.level().getServer()).lookup(variant.material());
        if (!lookup.permitsGrowth()) return fail(serverPlayer, lookup.warningKey());
        var definition = lookup.material().orElseThrow().definition();
        var bedTier = ModBlocks.growBedTier(level.getBlockState(context.getClickedPos()).getBlock());
        if (bedTier == null || bedTier.ordinal() < definition.tier().ordinal()) {
            return fail(serverPlayer, "warning.neroagriculture.wrong_bed");
        }
        if (definition.gate() != null && !ProgressionGates.isOpen(serverPlayer, definition.gate())) {
            return fail(serverPlayer, "warning.neroagriculture.gate_closed");
        }
        if (definition.worldRestriction() != null
                && !definition.worldRestriction().dimension().equals(level.dimension().identifier())) {
            return fail(serverPlayer, "warning.neroagriculture.wrong_dimension");
        }
        var cropPos = context.getClickedPos().above();
        if (!level.getBlockState(cropPos).canBeReplaced()) return InteractionResult.FAIL;
        if (!level.setBlock(cropPos, ModBlocks.RESOURCE_CROP.get().defaultBlockState(), 3)) return InteractionResult.FAIL;
        if (!(level.getBlockEntity(cropPos) instanceof ResourceCropBlockEntity crop)) {
            level.removeBlock(cropPos, false);
            return InteractionResult.FAIL;
        }
        int harvests = stack.getOrDefault(ModDataComponents.HARVEST_COUNT.get(), 0);
        crop.setVariant(new CropVariantState(CropVariantState.CURRENT_FORMAT, definition.id(), definition.tier(), harvests));
        crop.setGenetics(stack.get(ModDataComponents.GENETICS.get()));
        if (!serverPlayer.getAbilities().instabuild) stack.shrink(1);
        return InteractionResult.SUCCESS;
    }

    private static InteractionResult fail(ServerPlayer player, String key) {
        player.sendSystemMessage(Component.translatable(key));
        return InteractionResult.FAIL;
    }
}
