package za.co.neroland.neroagriculture.content;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import za.co.neroland.neroagriculture.food.FoodCatalog;
import za.co.neroland.neroagriculture.food.FoodEffects;
import za.co.neroland.neroagriculture.registry.ModDataComponents;

/**
 * One finite edible item that carries its species in a component. Vanilla drives hunger/animation from the
 * baseline food component; the bounded signature effect is applied server-side from the server catalog, so a
 * forged component can never grant an out-of-cap effect.
 */
public final class SpeciesFoodItem extends Item {
    public SpeciesFoodItem(Properties properties) {
        super(properties);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        ItemStack result = super.finishUsingItem(stack, level, entity);
        if (!level.isClientSide() && entity instanceof ServerPlayer player) {
            SpeciesVariant variant = stack.get(ModDataComponents.SPECIES_VARIANT.get());
            if (variant != null) {
                za.co.neroland.neroagriculture.genetics.Genetics genetics = stack.getOrDefault(
                        ModDataComponents.GENETICS.get(), za.co.neroland.neroagriculture.genetics.Genetics.EMPTY);
                FoodCatalog.lookup(player.level().getServer(), variant.species())
                        .ifPresent(definition -> FoodEffects.applyTo(player, definition, genetics));
            }
        }
        return result;
    }

    /**
     * Named from the species id's leaf path alone — the same client/server name-parity technique as
     * {@link SpeciesSeedItem#variantLabel}: {@code FoodCatalog.forServer(null)} is the built-in-only
     * snapshot on a dedicated server's clients, so a catalog-derived name would disagree between the
     * sides (container titles, {@code /give} feedback and {@code AnvilMenu.createResult} all run
     * server-side). The catalogued display name stays a client-only tooltip concern.
     */
    @Override
    public Component getName(ItemStack stack) {
        SpeciesVariant variant = stack.get(ModDataComponents.SPECIES_VARIANT.get());
        if (variant != null) {
            return Component.translatable("item.neroagriculture.engineered_food.named",
                    MaterialVariantItem.materialName(variant.species()));
        }
        return super.getName(stack);
    }
}
