package za.co.neroland.neroagriculture.recipe.compat;

// Minecraft 1.21.x only (empty on 26.x). Those versions have no net.minecraft.world.item.ItemStackTemplate;
// this record has the same shape for the parts FabricationRecipe and its callers use, backed by a plain
// ItemStack (same JSON form: {"id": .., "count": ..}). The build rewrites
// `import net.minecraft.world.item.ItemStackTemplate;` to this class on those lines.

//? if <26.1 {
/*import com.mojang.serialization.Codec;

import net.minecraft.core.Holder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public record ItemStackTemplate(ItemStack stack) {

    public static final Codec<ItemStackTemplate> CODEC =
            ItemStack.STRICT_CODEC.xmap(ItemStackTemplate::new, ItemStackTemplate::stack);

    public static final StreamCodec<RegistryFriendlyByteBuf, ItemStackTemplate> STREAM_CODEC =
            ItemStack.STREAM_CODEC.map(ItemStackTemplate::new, ItemStackTemplate::stack);

    // A fresh stack, safe to hand out and mutate.
    public ItemStack create() {
        return stack.copy();
    }

    public Holder<Item> item() {
        return stack.getItemHolder();
    }

    public int count() {
        return stack.getCount();
    }
}
*///?}
