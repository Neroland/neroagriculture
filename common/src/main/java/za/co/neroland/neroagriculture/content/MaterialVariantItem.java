package za.co.neroland.neroagriculture.content;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.TooltipFlag;

import za.co.neroland.neroagriculture.catalog.ClientMaterialCatalog;
import za.co.neroland.neroagriculture.registry.ModDataComponents;

/** Finite component-backed item with fail-closed client catalog feedback. */
public class MaterialVariantItem extends Item {
    public MaterialVariantItem(Properties properties) { super(properties); }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
            Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltip, flag);
        MaterialVariant variant = stack.get(ModDataComponents.MATERIAL_VARIANT.get());
        if (variant == null) return;
        tooltip.accept(Component.literal(variant.material().toString()).withStyle(ChatFormatting.GRAY));
        var metadata = ClientMaterialCatalog.entries().get(variant.material());
        if (metadata == null) {
            tooltip.accept(Component.translatable("warning.neroagriculture.unknown_material")
                    .withStyle(ChatFormatting.RED));
        } else {
            tooltip.accept(Component.translatable("tooltip.neroagriculture.tier", metadata.tier().name())
                    .withStyle(ChatFormatting.DARK_GRAY));
            tooltip.accept(Component.translatable("tooltip.neroagriculture.bed", metadata.tier().name())
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
        tooltip.accept(Component.translatable("tooltip.neroagriculture.harvests",
                stack.getOrDefault(ModDataComponents.HARVEST_COUNT.get(), 0)).withStyle(ChatFormatting.DARK_GRAY));
    }
}
