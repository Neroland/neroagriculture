package za.co.neroland.neroagriculture.registry;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import com.mojang.serialization.Codec;
import net.minecraft.network.codec.ByteBufCodecs;

import za.co.neroland.neroagriculture.NeroAgricultureCommon;
import za.co.neroland.neroagriculture.content.MaterialVariant;
import za.co.neroland.neroagriculture.content.SpeciesVariant;
import za.co.neroland.neroagriculture.content.EssenceCharge;
import za.co.neroland.nerolandcore.registry.RegistrationProvider;
import za.co.neroland.nerolandcore.registry.RegistrationProvider.RegistryEntry;

/** Component-backed variants keep the registered item catalog finite. */
public final class ModDataComponents {
    public static final RegistrationProvider<DataComponentType<?>> COMPONENTS =
            RegistrationProvider.get(Registries.DATA_COMPONENT_TYPE, NeroAgricultureCommon.MOD_ID);

    public static final RegistryEntry<DataComponentType<MaterialVariant>> MATERIAL_VARIANT =
            COMPONENTS.register("material_variant", key -> DataComponentType.<MaterialVariant>builder()
                    .persistent(MaterialVariant.CODEC)
                    .networkSynchronized(MaterialVariant.STREAM_CODEC)
                    .build());

    public static final RegistryEntry<DataComponentType<Integer>> HARVEST_COUNT =
            COMPONENTS.register("harvest_count", key -> DataComponentType.<Integer>builder()
                    .persistent(Codec.intRange(0, za.co.neroland.neroagriculture.crop.CropVariantState.MAX_HARVEST_COUNT))
                    .networkSynchronized(ByteBufCodecs.VAR_INT)
                    .build());

    public static final RegistryEntry<DataComponentType<za.co.neroland.neroagriculture.genetics.Genetics>> GENETICS =
            COMPONENTS.register("genetics", key -> DataComponentType.<za.co.neroland.neroagriculture.genetics.Genetics>builder()
                    .persistent(za.co.neroland.neroagriculture.genetics.Genetics.CODEC)
                    .networkSynchronized(za.co.neroland.neroagriculture.genetics.Genetics.STREAM_CODEC)
                    .build());

    public static final RegistryEntry<DataComponentType<SpeciesVariant>> SPECIES_VARIANT =
            COMPONENTS.register("species_variant", key -> DataComponentType.<SpeciesVariant>builder()
                    .persistent(SpeciesVariant.CODEC)
                    .networkSynchronized(SpeciesVariant.STREAM_CODEC)
                    .build());

    public static final RegistryEntry<DataComponentType<EssenceCharge>> ESSENCE_CHARGE =
            COMPONENTS.register("essence_charge", key -> DataComponentType.<EssenceCharge>builder()
                    .persistent(EssenceCharge.CODEC)
                    .networkSynchronized(EssenceCharge.STREAM_CODEC)
                    .build());

    private ModDataComponents() { }
    public static void init() { }
}
