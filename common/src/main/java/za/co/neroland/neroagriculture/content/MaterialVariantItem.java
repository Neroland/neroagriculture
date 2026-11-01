package za.co.neroland.neroagriculture.content;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
//? if >=1.21.5 {
import net.minecraft.world.item.component.TooltipDisplay;
//?}
import net.minecraft.world.item.TooltipFlag;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.neroagriculture.catalog.ClientMaterialCatalog;
import za.co.neroland.neroagriculture.registry.ModDataComponents;

/** Finite component-backed item with fail-closed client catalog feedback. */
public class MaterialVariantItem extends Item {
    public MaterialVariantItem(Properties properties) { super(properties); }

    /** Display as "<Material> Fragment" / "<Material> Seed" (e.g. "Iron Seed") from the variant. */
    @Override public Component getName(ItemStack stack) {
        Component label = variantLabel(stack);
        if (label == null) return super.getName(stack);
        return Component.translatable(nameKey(), label);
    }

    /** Translation key with a single {@code %s} for the variant name. */
    protected String nameKey() { return "item.neroagriculture.resource_fragment.named"; }

    /**
     * The substituted name part for {@link #nameKey()}, or {@code null} when this stack carries no variant
     * identity at all. Subclasses backed by a different identity component override this.
     *
     * <p>Derived from the stack's own component alone — never from a synced catalog — so the name a
     * dedicated server computes for container titles, {@code /give} feedback and
     * {@code AnvilMenu.createResult} is the same name the client renders.</p>
     */
    @Nullable
    protected Component variantLabel(ItemStack stack) {
        MaterialVariant variant = stack.get(ModDataComponents.MATERIAL_VARIANT.get());
        return variant == null ? null : Component.literal(materialName(variant.material()));
    }

    /**
     * Title-case the material id's leaf path: {@code c:iron -> "Iron"}, {@code c:nether_star -> "Nether Star"}.
     * Shared with the compatibility panels through {@link MaterialNames}.
     */
    public static String materialName(net.minecraft.resources.Identifier id) {
        return MaterialNames.leafName(id);
    }

    //? if >=1.21.5 {
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
            Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltip, flag);
    //?} else {
    /*@Override
    public void appendHoverText(ItemStack stack, TooltipContext context, java.util.List<Component> lines,
            TooltipFlag flag) {
        super.appendHoverText(stack, context, lines, flag);
        Consumer<Component> tooltip = lines::add;
    *///?}
        if (!appendVariantDetails(stack, tooltip)) return;
        tooltip.accept(Component.translatable("tooltip.neroagriculture.harvests",
                stack.getOrDefault(ModDataComponents.HARVEST_COUNT.get(), 0)).withStyle(ChatFormatting.GRAY));
        if (this instanceof ResourceSeedItem || this instanceof SpeciesSeedItem) {
            var genetics = stack.getOrDefault(ModDataComponents.GENETICS.get(),
                    za.co.neroland.neroagriculture.genetics.Genetics.EMPTY);
            tooltip.accept(Component.translatable("tooltip.neroagriculture.genetics_growth", genetics.yield(),
                    genetics.speed(), genetics.hardiness()).withStyle(ChatFormatting.GRAY));
            tooltip.accept(Component.translatable("tooltip.neroagriculture.genetics_special", genetics.oxygenOutput(),
                    genetics.foodPotency(), genetics.total()).withStyle(ChatFormatting.GRAY));
        }
    }

    /**
     * Identity and synced-catalog lines for this stack's variant flavour. Returns {@code false} when the
     * stack carries no variant identity, in which case the shared harvest line is suppressed too.
     * Subclasses backed by a different identity component override this.
     */
    protected boolean appendVariantDetails(ItemStack stack, Consumer<Component> tooltip) {
        MaterialVariant variant = stack.get(ModDataComponents.MATERIAL_VARIANT.get());
        if (variant == null) return false;
        tooltip.accept(Component.literal(variant.material().toString()).withStyle(ChatFormatting.GRAY));
        var metadata = ClientMaterialCatalog.entries().get(variant.material());
        if (metadata == null) {
            tooltip.accept(Component.translatable("warning.neroagriculture.unknown_material")
                    .withStyle(ChatFormatting.RED));
        } else {
            tooltip.accept(Component.translatable("tooltip.neroagriculture.tier", metadata.tier().name())
                    .withStyle(ChatFormatting.GRAY));
            tooltip.accept(Component.translatable("tooltip.neroagriculture.bed", metadata.tier().name())
                    .withStyle(ChatFormatting.GRAY));
            if (this instanceof ResourceSeedItem) {
                tooltip.accept(Component.translatable("tooltip.neroagriculture.base_yield", metadata.minimumYield(),
                        metadata.maximumYield(), metadata.rampHarvests()).withStyle(ChatFormatting.GRAY));
                tooltip.accept(Component.translatable("tooltip.neroagriculture.conversion", metadata.conversion())
                        .withStyle(ChatFormatting.GRAY));
                if (!metadata.dimension().isEmpty()) {
                    tooltip.accept(Component.translatable("tooltip.neroagriculture.world", metadata.dimension())
                            .withStyle(ChatFormatting.GRAY));
                }
                if (!metadata.gate().isEmpty()) {
                    tooltip.accept(Component.translatable("tooltip.neroagriculture.required_gate", metadata.gate())
                            .withStyle(ChatFormatting.GRAY));
                }
            }
        }
        return true;
    }
}
