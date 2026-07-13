package za.co.neroland.neroagriculture.recipe;

import java.util.Optional;
import java.util.function.Supplier;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;

import za.co.neroland.neroagriculture.content.FragmentTier;

/** Datapack fabrication recipe with bounded costs and optional material/tier identity. */
public final class FabricationRecipe implements Recipe<SingleRecipeInput> {
    private final Supplier<RecipeType<FabricationRecipe>> type;
    private final Supplier<RecipeSerializer<FabricationRecipe>> serializer;
    private final CommonInfo commonInfo;
    private final Ingredient ingredient;
    private final ItemStackTemplate result;
    private final int inputCount;
    private final int energy;
    private final int ticks;
    private final Optional<FragmentTier> family;
    private final Optional<Identifier> material;

    public FabricationRecipe(Supplier<RecipeType<FabricationRecipe>> type,
            Supplier<RecipeSerializer<FabricationRecipe>> serializer, CommonInfo commonInfo,
            Ingredient ingredient, ItemStackTemplate result, int inputCount, int energy, int ticks,
            Optional<FragmentTier> family, Optional<Identifier> material) {
        if (inputCount < 1 || inputCount > 64) throw new IllegalArgumentException("input_count outside 1-64");
        if (energy < 0 || energy > 1_000_000) throw new IllegalArgumentException("energy outside 0-1000000");
        if (ticks < 1 || ticks > 72_000) throw new IllegalArgumentException("ticks outside 1-72000");
        this.type = type;
        this.serializer = serializer;
        this.commonInfo = commonInfo;
        this.ingredient = ingredient;
        this.result = result;
        this.inputCount = inputCount;
        this.energy = energy;
        this.ticks = ticks;
        this.family = family;
        this.material = material;
    }

    public static MapCodec<FabricationRecipe> mapCodec(Supplier<RecipeType<FabricationRecipe>> type,
            Supplier<RecipeSerializer<FabricationRecipe>> serializer) {
        return RecordCodecBuilder.mapCodec(instance -> instance.group(
                CommonInfo.MAP_CODEC.forGetter(FabricationRecipe::commonInfo),
                Ingredient.CODEC.fieldOf("ingredient").forGetter(FabricationRecipe::ingredient),
                ItemStackTemplate.CODEC.fieldOf("result").forGetter(FabricationRecipe::resultTemplate),
                Codec.intRange(1, 64).optionalFieldOf("input_count", 1).forGetter(FabricationRecipe::inputCount),
                Codec.intRange(0, 1_000_000).optionalFieldOf("energy", 800).forGetter(FabricationRecipe::energy),
                Codec.intRange(1, 72_000).optionalFieldOf("ticks", 100).forGetter(FabricationRecipe::ticks),
                FragmentTier.CODEC.optionalFieldOf("family").forGetter(FabricationRecipe::family),
                Identifier.CODEC.optionalFieldOf("material").forGetter(FabricationRecipe::material)
        ).apply(instance, (common, ingredient, result, count, energy, ticks, family, material) ->
                new FabricationRecipe(type, serializer, common, ingredient, result, count, energy, ticks,
                        family, material)));
    }

    public static StreamCodec<RegistryFriendlyByteBuf, FabricationRecipe> streamCodec(
            Supplier<RecipeType<FabricationRecipe>> type,
            Supplier<RecipeSerializer<FabricationRecipe>> serializer) {
        return StreamCodec.of((buffer, recipe) -> {
            CommonInfo.STREAM_CODEC.encode(buffer, recipe.commonInfo);
            Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, recipe.ingredient);
            ItemStackTemplate.STREAM_CODEC.encode(buffer, recipe.result);
            buffer.writeVarInt(recipe.inputCount);
            buffer.writeVarInt(recipe.energy);
            buffer.writeVarInt(recipe.ticks);
            ByteBufCodecs.optional(ByteBufCodecs.fromCodecWithRegistries(FragmentTier.CODEC))
                    .encode(buffer, recipe.family);
            ByteBufCodecs.optional(Identifier.STREAM_CODEC).encode(buffer, recipe.material);
        }, buffer -> new FabricationRecipe(type, serializer,
                CommonInfo.STREAM_CODEC.decode(buffer), Ingredient.CONTENTS_STREAM_CODEC.decode(buffer),
                ItemStackTemplate.STREAM_CODEC.decode(buffer), buffer.readVarInt(), buffer.readVarInt(),
                buffer.readVarInt(),
                ByteBufCodecs.optional(ByteBufCodecs.fromCodecWithRegistries(FragmentTier.CODEC)).decode(buffer),
                ByteBufCodecs.optional(Identifier.STREAM_CODEC).decode(buffer)));
    }

    @Override public boolean matches(SingleRecipeInput input, Level level) {
        return input.item().getCount() >= inputCount && ingredient.test(input.item());
    }
    @Override public ItemStack assemble(SingleRecipeInput input) { return result.create(); }
    @Override public boolean showNotification() { return commonInfo.showNotification(); }
    @Override public String group() { return ""; }
    @Override public RecipeSerializer<FabricationRecipe> getSerializer() { return serializer.get(); }
    @Override public RecipeType<FabricationRecipe> getType() { return type.get(); }
    @Override public PlacementInfo placementInfo() { return PlacementInfo.create(ingredient); }
    @Override public RecipeBookCategory recipeBookCategory() { return RecipeBookCategories.CRAFTING_MISC; }

    public CommonInfo commonInfo() { return commonInfo; }
    public Ingredient ingredient() { return ingredient; }
    public ItemStackTemplate resultTemplate() { return result; }
    public int inputCount() { return inputCount; }
    public int energy() { return energy; }
    public int ticks() { return ticks; }
    public Optional<FragmentTier> family() { return family; }
    public Optional<Identifier> material() { return material; }
}
