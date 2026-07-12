package za.co.neroland.neroagriculture.neoforge;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

import za.co.neroland.neroagriculture.client.FoundationMachineScreen;
import za.co.neroland.neroagriculture.registry.ModMenuTypes;

/** NeoForge client-only fabrication screen wiring. */
public final class NeoForgeClientSetup {
    private NeoForgeClientSetup() { }
    public static void init(IEventBus bus) { bus.addListener(NeoForgeClientSetup::screens); }
    private static void screens(RegisterMenuScreensEvent event) {
        event.register(ModMenuTypes.FOUNDATION_MACHINE.get(), FoundationMachineScreen::new);
    }
}
