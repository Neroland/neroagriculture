package za.co.neroland.neroagriculture.client;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;

import za.co.neroland.neroagriculture.registry.ModMenuTypes;

/**
 * The canonical menu-to-screen binding table, iterated by all three loader client setups so a new menu
 * is wired once here instead of three times. CLIENT-ONLY: this class references screen classes, so it
 * must only ever be loaded from a client entry point (all three loader setups are client-only contexts).
 */
public final class ScreenBindings {
    private ScreenBindings() { }

    /** Loader-neutral mirror of the vanilla screen-constructor shape. */
    @FunctionalInterface
    public interface ScreenFactory<M extends AbstractContainerMenu, U extends AbstractContainerScreen<M>> {
        U create(M menu, Inventory inventory, Component title);
    }

    /** Adapts one loader's menu-screen registration call (vanilla {@code MenuScreens.register} or event). */
    @FunctionalInterface
    public interface Registrar {
        <M extends AbstractContainerMenu, U extends AbstractContainerScreen<M>> void register(
                MenuType<M> type, ScreenFactory<M, U> factory);
    }

    /** Register every NeroAgriculture screen against its menu type. */
    public static void registerAll(Registrar registrar) {
        registrar.register(ModMenuTypes.FOUNDATION_MACHINE.get(), FoundationMachineScreen::new);
        registrar.register(ModMenuTypes.GENETICS_STATION.get(), GeneticsStationScreen::new);
        registrar.register(ModMenuTypes.CROP_TOWER_CONTROLLER.get(), CropTowerScreen::new);
        registrar.register(ModMenuTypes.STATUS_CONTROLLER.get(), StatusScreen::new);
        registrar.register(ModMenuTypes.AREA_MACHINE.get(), AreaMachineScreen::new);
        registrar.register(ModMenuTypes.CONVERTER.get(), ProcessorScreen::new);
        registrar.register(ModMenuTypes.PROCESSOR.get(), ProcessorScreen::new);
        registrar.register(ModMenuTypes.GROW_BED.get(), GrowBedScreen::new);
    }
}
