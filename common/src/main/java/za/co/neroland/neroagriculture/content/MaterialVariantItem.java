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

    /** Display as "<Material> Fragment" / "<Material> Seed" (e.g. "Iron Seed") from the variant. */
    @Override public Component getName(ItemStack stack) {
        MaterialVariant variant = stack.get(ModDataComponents.MATERIAL_VARIANT.get());
        if (variant == null) return super.getName(stack);
        return Component.translatable(nameKey(), materialName(variant.material()));
    }

    /** Translation key with a single {@code %s} for the material name. */
    protected String nameKey() { return "item.neroagriculture.resource_fragment.named"; }

    /** Title-case the material id's leaf path: {@code c:iron -> "Iron"}, {@code c:nether_star -> "Nether Star"}. */
    public static String materialName(net.minecraft.resources.Identifier id) {
        String path = id.getPath();
        int slash = path.lastIndexOf('/');
        if (slash >= 0) path = path.substring(slash + 1);
        StringBuilder sb = new StringBuilder();
        for (String word : path.split("_")) {
            if (word.isEmpty()) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return sb.toString();
    }

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
