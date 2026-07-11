package za.co.neroland.neroagriculture.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;

import za.co.neroland.neroagriculture.NeroAgricultureCommon;
import za.co.neroland.nerolandcore.registry.RegistrationProvider;

/** Reserved finite recipe registration surface; concrete serializers are introduced with machine logic. */
public final class ModRecipeSerializers {
    public static final RegistrationProvider<RecipeSerializer<?>> SERIALIZERS =
            RegistrationProvider.get(Registries.RECIPE_SERIALIZER, NeroAgricultureCommon.MOD_ID);
    private ModRecipeSerializers() { }
    public static void init() { }
}
