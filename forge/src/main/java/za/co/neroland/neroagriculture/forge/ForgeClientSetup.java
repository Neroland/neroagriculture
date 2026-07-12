package za.co.neroland.neroagriculture.forge;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

import za.co.neroland.neroagriculture.client.FoundationMachineScreen;
import za.co.neroland.neroagriculture.registry.ModMenuTypes;

/** Forge client-only fabrication screen wiring. */
public final class ForgeClientSetup {
    private ForgeClientSetup() { }
    public static void init(BusGroup bus) {
        FMLClientSetupEvent.getBus(bus).addListener(event -> event.enqueueWork(() ->
                MenuScreens.register(ModMenuTypes.FOUNDATION_MACHINE.get(), FoundationMachineScreen::new)));
    }
}
