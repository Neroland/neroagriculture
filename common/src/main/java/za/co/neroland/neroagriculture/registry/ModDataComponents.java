package za.co.neroland.neroagriculture.registry;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;

import za.co.neroland.neroagriculture.NeroAgricultureCommon;
import za.co.neroland.neroagriculture.content.MaterialVariant;
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

    private ModDataComponents() { }
    public static void init() { }
}
