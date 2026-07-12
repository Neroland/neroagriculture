package za.co.neroland.neroagriculture.content;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import za.co.neroland.neroagriculture.registry.ModDataComponents;

/** Charged blank whose tier identity is visible and persistent. */
public final class ChargedSeedItem extends Item {
    public ChargedSeedItem(Properties properties) { super(properties); }
    @Override public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
            Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltip, flag);
        EssenceCharge charge = stack.get(ModDataComponents.ESSENCE_CHARGE.get());
        if (charge == null) {
            tooltip.accept(Component.translatable("warning.neroagriculture.invalid_charge")
                    .withStyle(ChatFormatting.RED));
        } else {
            tooltip.accept(Component.translatable("tooltip.neroagriculture.charge", charge.family().name())
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
