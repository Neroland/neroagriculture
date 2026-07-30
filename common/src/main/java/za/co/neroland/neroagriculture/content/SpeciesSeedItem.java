package za.co.neroland.neroagriculture.content;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.neroagriculture.catalog.ClientSpeciesCatalog;
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

    @Override protected String nameKey() {
        return kind == FoodDefinition.Kind.ALIEN
                ? "item.neroagriculture.alien_seed.named" : "item.neroagriculture.food_seed.named";
    }

    /**
     * Species seeds are identified by {@link ModDataComponents#SPECIES_VARIANT}, not by the material
     * component, but the <em>name</em> is still derived from the id's leaf path alone.
     *
     * <p>{@link ClientSpeciesCatalog} is populated by a clientbound sync and is permanently empty on a
     * dedicated server, so reading it here would make {@code getHoverName()} disagree between the two
     * sides — container titles, {@code /give} feedback, item frames and the rename comparison in
     * {@code AnvilMenu.createResult} all run server-side. The catalogued display name is a tooltip
     * concern only, where a client-only source is fine (see {@link #appendVariantDetails}).</p>
     */
    @Nullable
    @Override protected Component variantLabel(ItemStack stack) {
        SpeciesVariant variant = stack.get(ModDataComponents.SPECIES_VARIANT.get());
        return variant == null ? null : Component.literal(materialName(variant.species()));
    }

    @Override
    protected boolean appendVariantDetails(ItemStack stack, Consumer<Component> tooltip) {
        SpeciesVariant variant = stack.get(ModDataComponents.SPECIES_VARIANT.get());
        if (variant == null) return false;
        tooltip.accept(Component.literal(variant.species().toString()).withStyle(ChatFormatting.GRAY));
        var metadata = ClientSpeciesCatalog.entries().get(variant.species());
        if (metadata == null) {
            tooltip.accept(Component.translatable("warning.neroagriculture.unknown_species")
                    .withStyle(ChatFormatting.RED));
        } else {
            // Catalogue-sourced display name: client-only, and only worth a line when the datapack's
            // display key actually resolves to something the item name (leaf path) does not already say.
            Component catalogued = MaterialNames.display(variant.species(), metadata.displayKey());
            if (!catalogued.getString().equals(materialName(variant.species()))) {
                tooltip.accept(catalogued.copy().withStyle(ChatFormatting.GRAY));
            }
            tooltip.accept(Component.translatable("tooltip.neroagriculture.kind",
                    Component.translatable(metadata.kind() == FoodDefinition.Kind.ALIEN
                            ? "tooltip.neroagriculture.kind.alien" : "tooltip.neroagriculture.kind.food"))
                    .withStyle(ChatFormatting.DARK_GRAY));
            tooltip.accept(Component.translatable("tooltip.neroagriculture.tier", metadata.tier().name())
                    .withStyle(ChatFormatting.DARK_GRAY));
            // Species-specific wording: a species seed is planted by hand or by a Planter, never through a
            // machine seed slot, so it must not read as "put this in the slot of a bed of tier X".
            tooltip.accept(Component.translatable("tooltip.neroagriculture.bed_species", metadata.tier().name())
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
        // The grow bed and crop tower seed slots accept the Resource Seed alone, so the bed line above
        // describes what to right-click on rather than a slot to fill. The Planter does accept species
        // seeds (see AreaFarming#plantSpecies), so this must not claim hand-planting is the only route.
        tooltip.accept(Component.translatable("tooltip.neroagriculture.no_bed_slot")
                .withStyle(ChatFormatting.DARK_GRAY));
        return true;
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
