package za.co.neroland.neroagriculture.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

import za.co.neroland.neroagriculture.NeroAgricultureCommon;
import za.co.neroland.neroagriculture.recipe.FabricationRecipe;
import za.co.neroland.nerolandcore.registry.RegistrationProvider;
import za.co.neroland.nerolandcore.registry.RegistrationProvider.RegistryEntry;

/** Stage 5 datapack fabrication recipe types and serializers. */
public final class ModRecipeSerializers {
    public static final RegistrationProvider<RecipeType<?>> TYPES =
            RegistrationProvider.get(Registries.RECIPE_TYPE, NeroAgricultureCommon.MOD_ID);
    public static final RegistrationProvider<RecipeSerializer<?>> SERIALIZERS =
            RegistrationProvider.get(Registries.RECIPE_SERIALIZER, NeroAgricultureCommon.MOD_ID);

    public static final RegistryEntry<RecipeType<FabricationRecipe>> EXTRACTION = type("material_extraction");
    public static final RegistryEntry<RecipeType<FabricationRecipe>> INFUSING = type("essence_infusing");
    public static final RegistryEntry<RecipeType<FabricationRecipe>> SYNTHESIZING = type("seed_synthesizing");
    public static final RegistryEntry<RecipeType<FabricationRecipe>> RESEARCHING = type("seed_researching");
    public static final RegistryEntry<RecipeType<FabricationRecipe>> CONVERSION = type("material_conversion");

    public static final RegistryEntry<RecipeSerializer<FabricationRecipe>> EXTRACTION_SERIALIZER =
            serializer("material_extraction", EXTRACTION);
    public static final RegistryEntry<RecipeSerializer<FabricationRecipe>> INFUSING_SERIALIZER =
            serializer("essence_infusing", INFUSING);
    public static final RegistryEntry<RecipeSerializer<FabricationRecipe>> SYNTHESIZING_SERIALIZER =
            serializer("seed_synthesizing", SYNTHESIZING);
    public static final RegistryEntry<RecipeSerializer<FabricationRecipe>> RESEARCHING_SERIALIZER =
            serializer("seed_researching", RESEARCHING);
    public static final RegistryEntry<RecipeSerializer<FabricationRecipe>> CONVERSION_SERIALIZER =
            serializer("material_conversion", CONVERSION);

    private static RegistryEntry<RecipeType<FabricationRecipe>> type(String name) {
        return TYPES.register(name, key -> new RecipeType<FabricationRecipe>() {
            @Override public String toString() { return key.identifier().toString(); }
        });
    }

    private static RegistryEntry<RecipeSerializer<FabricationRecipe>> serializer(String name,
            RegistryEntry<RecipeType<FabricationRecipe>> type) {
        java.util.concurrent.atomic.AtomicReference<RecipeSerializer<FabricationRecipe>> self =
                new java.util.concurrent.atomic.AtomicReference<>();
        RegistryEntry<RecipeSerializer<FabricationRecipe>> entry = SERIALIZERS.register(name, key -> {
            RecipeSerializer<FabricationRecipe> serializer = new RecipeSerializer<>(
                    FabricationRecipe.mapCodec(type::get, self::get),
                    FabricationRecipe.streamCodec(type::get, self::get));
            self.set(serializer);
            return serializer;
        });
        return entry;
    }
    private ModRecipeSerializers() { }
    public static void init() { }
}
