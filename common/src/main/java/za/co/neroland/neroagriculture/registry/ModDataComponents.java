package za.co.neroland.neroagriculture.registry;

import com.mojang.serialization.Codec;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import za.co.neroland.neroagriculture.content.MaterialVariant;
import za.co.neroland.neroagriculture.content.SpeciesVariant;
import za.co.neroland.neroagriculture.content.FragmentCharge;
import za.co.neroland.neroagriculture.crop.CropVariantState;
import za.co.neroland.neroagriculture.genetics.Genetics;
//? if >=1.20.5 {
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;

import za.co.neroland.neroagriculture.NeroAgricultureCommon;
import za.co.neroland.nerolandcore.registry.RegistrationProvider;
//?} else {
/*import net.minecraft.nbt.NbtOps;
*///?}

/** Keeps variant data attached to item stacks across Minecraft versions. */
public final class ModDataComponents {
    //? if >=1.20.5 {
    public static final RegistrationProvider<DataComponentType<?>> COMPONENTS =
            RegistrationProvider.get(Registries.DATA_COMPONENT_TYPE, NeroAgricultureCommon.MOD_ID);

    public static final StackData<MaterialVariant> MATERIAL_VARIANT =
            new StackData<>(COMPONENTS.register("material_variant", key -> DataComponentType.<MaterialVariant>builder()
                    .persistent(MaterialVariant.CODEC)
                    .networkSynchronized(MaterialVariant.STREAM_CODEC)
                    .build()));

    public static final StackData<Integer> HARVEST_COUNT =
            new StackData<>(COMPONENTS.register("harvest_count", key -> DataComponentType.<Integer>builder()
                    .persistent(Codec.intRange(0, CropVariantState.MAX_HARVEST_COUNT))
                    .networkSynchronized(ByteBufCodecs.VAR_INT)
                    .build()));

    public static final StackData<Genetics> GENETICS =
            new StackData<>(COMPONENTS.register("genetics", key -> DataComponentType.<Genetics>builder()
                    .persistent(Genetics.CODEC)
                    .networkSynchronized(Genetics.STREAM_CODEC)
                    .build()));

    public static final StackData<SpeciesVariant> SPECIES_VARIANT =
            new StackData<>(COMPONENTS.register("species_variant", key -> DataComponentType.<SpeciesVariant>builder()
                    .persistent(SpeciesVariant.CODEC)
                    .networkSynchronized(SpeciesVariant.STREAM_CODEC)
                    .build()));

    public static final StackData<FragmentCharge> FRAGMENT_CHARGE =
            new StackData<>(COMPONENTS.register("fragment_charge", key -> DataComponentType.<FragmentCharge>builder()
                    .persistent(FragmentCharge.CODEC)
                    .networkSynchronized(FragmentCharge.STREAM_CODEC)
                    .build()));

    //?} else {
    /*public static final StackData<MaterialVariant> MATERIAL_VARIANT =
            new StackData<>("neroagriculture:material_variant", MaterialVariant.CODEC);
    public static final StackData<Integer> HARVEST_COUNT =
            new StackData<>("neroagriculture:harvest_count", Codec.intRange(0, CropVariantState.MAX_HARVEST_COUNT));
    public static final StackData<Genetics> GENETICS =
            new StackData<>("neroagriculture:genetics", Genetics.CODEC);
    public static final StackData<SpeciesVariant> SPECIES_VARIANT =
            new StackData<>("neroagriculture:species_variant", SpeciesVariant.CODEC);
    public static final StackData<FragmentCharge> FRAGMENT_CHARGE =
            new StackData<>("neroagriculture:fragment_charge", FragmentCharge.CODEC);
    *///?}

    //? if >=1.20.5 {
    public record StackData<T>(RegistrationProvider.RegistryEntry<DataComponentType<T>> entry) {
        public @Nullable T get(ItemStack stack) {
            return stack.get(entry.get());
        }

        public void set(ItemStack stack, T value) {
            stack.set(entry.get(), value);
        }

        public void remove(ItemStack stack) {
            stack.remove(entry.get());
        }
    //?} else {
    /*public record StackData<T>(String key, Codec<T> codec) {
        public @Nullable T get(ItemStack stack) {
            var tag = stack.getTag();
            if (tag == null || !tag.contains(key)) {
                return null;
            }
            return codec.parse(NbtOps.INSTANCE, tag.get(key)).result().orElse(null);
        }

        public void set(ItemStack stack, T value) {
            var encoded = codec.encodeStart(NbtOps.INSTANCE, value).result()
                    .orElseThrow(() -> new IllegalArgumentException("Cannot encode NeroAgriculture item data: " + key));
            stack.getOrCreateTag().put(key, encoded);
        }

        public void remove(ItemStack stack) {
            stack.removeTagKey(key);
        }
    *///?}

        public T getOrDefault(ItemStack stack, T fallback) {
            T current = get(stack);
            return current == null ? fallback : current;
        }
    }

    private ModDataComponents() { }
    public static void init() { }
}
