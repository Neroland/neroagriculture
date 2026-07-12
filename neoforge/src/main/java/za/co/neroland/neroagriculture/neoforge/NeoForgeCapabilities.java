package za.co.neroland.neroagriculture.neoforge;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.transfer.item.VanillaContainerWrapper;
import net.neoforged.neoforge.transfer.item.WorldlyContainerWrapper;

import za.co.neroland.neroagriculture.registry.ModBlockEntities;
import za.co.neroland.nerolandcore.platform.NeoForgeEnergyLookup;
import za.co.neroland.nerolandcore.platform.NeoForgeFluidLookup;

public final class NeoForgeCapabilities {
    private NeoForgeCapabilities() { }
    public static void register(IEventBus bus) { bus.addListener(NeoForgeCapabilities::onRegister); }
    private static void onRegister(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.Item.BLOCK, ModBlockEntities.FOUNDATION_MACHINE.get(),
                (be, side) -> side == null ? VanillaContainerWrapper.of(be) : new WorldlyContainerWrapper(be, side));
        event.registerBlockEntity(NeoForgeEnergyLookup.ENERGY, ModBlockEntities.FOUNDATION_MACHINE.get(),
                (be, side) -> be.sideConfig().energyView(side));
        event.registerBlockEntity(NeoForgeFluidLookup.FLUID, ModBlockEntities.FOUNDATION_MACHINE.get(),
                (be, side) -> be.sideConfig().fluidView(side));
        event.registerBlockEntity(NeoForgeEnergyLookup.ENERGY, ModBlockEntities.GROW_BED.get(),
                (be, side) -> be.sideConfig().energyView(side));
        event.registerBlockEntity(NeoForgeFluidLookup.FLUID, ModBlockEntities.GROW_BED.get(),
                (be, side) -> be.sideConfig().fluidView(side));
    }
}
