package za.co.neroland.neroagriculture.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

import za.co.neroland.neroagriculture.NeroAgricultureCommon;
import za.co.neroland.nerolandcore.registry.RegistrationProvider;
import za.co.neroland.nerolandcore.registry.RegistrationProvider.RegistryEntry;

/** NeroAgriculture's own creative tab; keeps its content out of the shared Neroland Core tab. */
public final class ModCreativeTabs {
    public static final RegistrationProvider<CreativeModeTab> CREATIVE_TABS =
            RegistrationProvider.get(Registries.CREATIVE_MODE_TAB, NeroAgricultureCommon.MOD_ID);

    public static final RegistryEntry<CreativeModeTab> NEROAGRICULTURE = CREATIVE_TABS.register("neroagriculture",
            key -> CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
                    .title(Component.translatable("itemGroup.neroagriculture"))
                    .icon(() -> new ItemStack(ModItems.RESOURCE_SEED.get()))
                    .displayItems((parameters, output) -> ModItems.populateTab(output::accept))
                    .build());

    private ModCreativeTabs() { }

    public static void init() { }
}
