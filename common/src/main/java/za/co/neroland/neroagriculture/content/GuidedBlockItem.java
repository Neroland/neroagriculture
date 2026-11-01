package za.co.neroland.neroagriculture.content;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;
//? if >=1.21.5 {
import net.minecraft.world.item.component.TooltipDisplay;
//?}

public final class GuidedBlockItem extends BlockItem {
    private final String guideKey;

    public GuidedBlockItem(Block block, Properties properties, String name) {
        super(block, properties);
        this.guideKey = BlockGuidance.guideKey(name);
    }

    //? if >=1.21.5 {
    @Override public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
            Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltip, flag);
    //?} else {
    /*@Override public void appendHoverText(ItemStack stack, TooltipContext context, java.util.List<Component> lines,
            TooltipFlag flag) {
        super.appendHoverText(stack, context, lines, flag);
        Consumer<Component> tooltip = lines::add;
    *///?}
        if (guideKey != null) tooltip.accept(Component.translatable(guideKey).withStyle(ChatFormatting.GRAY));
        if ("tooltip.neroagriculture.guide.seed_synthesizer".equals(guideKey)) {
            tooltip.accept(Component.translatable("tooltip.neroagriculture.guide.seed_synthesizer.more")
                    .withStyle(ChatFormatting.GRAY));
        }
    }
}
