package za.co.neroland.neroagriculture.forge;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

import za.co.neroland.neroagriculture.client.CropTintSource;
import za.co.neroland.neroagriculture.client.FoundationMachineScreen;
import za.co.neroland.neroagriculture.registry.ModBlocks;
import za.co.neroland.neroagriculture.registry.ModMenuTypes;

/** Forge client-only fabrication screen + colour-handler wiring. */
public final class ForgeClientSetup {
    private ForgeClientSetup() { }
    public static void init(BusGroup bus) {
        RegisterColorHandlersEvent.Block.BUS.addListener(event ->
                event.getBlockColors().register(java.util.List.of(new CropTintSource()), ModBlocks.RESOURCE_CROP.get()));
        FMLClientSetupEvent.getBus(bus).addListener(event -> event.enqueueWork(() -> {
            MenuScreens.register(ModMenuTypes.FOUNDATION_MACHINE.get(), FoundationMachineScreen::new);
            MenuScreens.register(ModMenuTypes.GENETICS_STATION.get(),
                    za.co.neroland.neroagriculture.client.GeneticsStationScreen::new);
            MenuScreens.register(ModMenuTypes.CROP_TOWER_CONTROLLER.get(),
                    za.co.neroland.neroagriculture.client.CropTowerScreen::new);
            MenuScreens.register(ModMenuTypes.STATUS_CONTROLLER.get(),
                    za.co.neroland.neroagriculture.client.StatusScreen::new);
            MenuScreens.register(ModMenuTypes.AREA_MACHINE.get(),
                    za.co.neroland.neroagriculture.client.AreaMachineScreen::new);
            MenuScreens.register(ModMenuTypes.CONVERTER.get(),
                    za.co.neroland.neroagriculture.client.ProcessorScreen::new);
            MenuScreens.register(ModMenuTypes.PROCESSOR.get(),
                    za.co.neroland.neroagriculture.client.ProcessorScreen::new);
            MenuScreens.register(ModMenuTypes.GROW_BED.get(),
                    za.co.neroland.neroagriculture.client.GrowBedScreen::new);
        }));
    }
}
