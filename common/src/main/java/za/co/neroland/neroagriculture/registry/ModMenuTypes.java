package za.co.neroland.neroagriculture.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;

import za.co.neroland.neroagriculture.NeroAgricultureCommon;
import za.co.neroland.neroagriculture.menu.FoundationMachineMenu;
import za.co.neroland.nerolandcore.registry.RegistrationProvider;
import za.co.neroland.nerolandcore.registry.RegistrationProvider.RegistryEntry;

public final class ModMenuTypes {
    public static final RegistrationProvider<MenuType<?>> MENUS =
            RegistrationProvider.get(Registries.MENU, NeroAgricultureCommon.MOD_ID);
    public static final RegistryEntry<MenuType<FoundationMachineMenu>> FOUNDATION_MACHINE = MENUS.register(
            "foundation_machine", key -> new MenuType<>(FoundationMachineMenu::new, FeatureFlags.VANILLA_SET));
    private ModMenuTypes() { }
    public static void init() { }
}
