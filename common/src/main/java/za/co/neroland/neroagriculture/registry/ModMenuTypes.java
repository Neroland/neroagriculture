package za.co.neroland.neroagriculture.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;

import za.co.neroland.neroagriculture.NeroAgricultureCommon;
import za.co.neroland.neroagriculture.menu.AreaMachineMenu;
import za.co.neroland.neroagriculture.menu.CropTowerMenu;
import za.co.neroland.neroagriculture.menu.FoundationMachineMenu;
import za.co.neroland.neroagriculture.menu.GeneticsStationMenu;
import za.co.neroland.neroagriculture.menu.StatusMenu;
import za.co.neroland.nerolandcore.registry.RegistrationProvider;
import za.co.neroland.nerolandcore.registry.RegistrationProvider.RegistryEntry;

public final class ModMenuTypes {
    public static final RegistrationProvider<MenuType<?>> MENUS =
            RegistrationProvider.get(Registries.MENU, NeroAgricultureCommon.MOD_ID);
    public static final RegistryEntry<MenuType<FoundationMachineMenu>> FOUNDATION_MACHINE = MENUS.register(
            "foundation_machine", key -> new MenuType<>(FoundationMachineMenu::new, FeatureFlags.VANILLA_SET));
    public static final RegistryEntry<MenuType<GeneticsStationMenu>> GENETICS_STATION = MENUS.register(
            "genetics_station", key -> new MenuType<>(GeneticsStationMenu::new, FeatureFlags.VANILLA_SET));
    public static final RegistryEntry<MenuType<CropTowerMenu>> CROP_TOWER_CONTROLLER = MENUS.register(
            "crop_tower_controller", key -> new MenuType<>(CropTowerMenu::new, FeatureFlags.VANILLA_SET));
    public static final RegistryEntry<MenuType<StatusMenu>> STATUS_CONTROLLER = MENUS.register(
            "status_controller", key -> new MenuType<>(StatusMenu::new, FeatureFlags.VANILLA_SET));
    public static final RegistryEntry<MenuType<AreaMachineMenu>> AREA_MACHINE = MENUS.register(
            "area_machine", key -> new MenuType<>(AreaMachineMenu::new, FeatureFlags.VANILLA_SET));
    public static final RegistryEntry<MenuType<za.co.neroland.neroagriculture.menu.ProcessorMenu>> CONVERTER = MENUS.register(
            "converter", key -> new MenuType<>(za.co.neroland.neroagriculture.menu.ProcessorMenu::converter, FeatureFlags.VANILLA_SET));
    public static final RegistryEntry<MenuType<za.co.neroland.neroagriculture.menu.ProcessorMenu>> PROCESSOR = MENUS.register(
            "processor", key -> new MenuType<>(za.co.neroland.neroagriculture.menu.ProcessorMenu::processor, FeatureFlags.VANILLA_SET));
    public static final RegistryEntry<MenuType<za.co.neroland.neroagriculture.menu.GrowBedMenu>> GROW_BED = MENUS.register(
            "grow_bed", key -> new MenuType<>(za.co.neroland.neroagriculture.menu.GrowBedMenu::new, FeatureFlags.VANILLA_SET));
    private ModMenuTypes() { }
    public static void init() { }
}
