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
//? if <1.20.5 {
//import com.mojang.serialization.codecs.RecordCodecBuilder;
//import net.minecraft.core.registries.BuiltInRegistries;
//?}
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public record ItemStackTemplate(ItemStack stack) {

    //? if >=1.20.5 {
    public static final Codec<ItemStackTemplate> CODEC =
            ItemStack.STRICT_CODEC.xmap(ItemStackTemplate::new, ItemStackTemplate::stack);

    public static final StreamCodec<RegistryFriendlyByteBuf, ItemStackTemplate> STREAM_CODEC =
            ItemStack.STREAM_CODEC.map(ItemStackTemplate::new, ItemStackTemplate::stack);
    //?} else {
    //public static final Codec<ItemStackTemplate> CODEC = RecordCodecBuilder.create(instance -> instance.group(
    //        BuiltInRegistries.ITEM.byNameCodec().fieldOf("id").forGetter(value -> value.stack().getItem()),
    //        Codec.intRange(1, 99).optionalFieldOf("count", 1).forGetter(ItemStackTemplate::count)
    //).apply(instance, (item, count) -> new ItemStackTemplate(new ItemStack(item, count))));
    //?}

    // A fresh stack, safe to hand out and mutate.
    public ItemStack create() {
        return stack.copy();
    }

    public Holder<Item> item() {
        //? if >=1.20.5 {
        return stack.getItemHolder();
        //?} else {
        //return BuiltInRegistries.ITEM.wrapAsHolder(stack.getItem());
        //?}
    }

    public int count() {
        return stack.getCount();
    }
}
*///?}
