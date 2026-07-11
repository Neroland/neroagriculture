package za.co.neroland.neroagriculture.fabric;

import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ContainerStorage;

import za.co.neroland.neroagriculture.registry.ModBlockEntities;
import za.co.neroland.nerolandcore.platform.FabricEnergyLookup;
import za.co.neroland.nerolandcore.platform.FabricFluidLookup;

/** Fabric exposure of the shared Core item/fluid/energy surfaces. */
public final class FabricCapabilities {
    private FabricCapabilities() { }
    public static void register() {
        ItemStorage.SIDED.registerForBlockEntity((be, side) -> ContainerStorage.of(be, side),
                ModBlockEntities.FOUNDATION_MACHINE.get());
        FabricEnergyLookup.ENERGY.registerForBlockEntity((be, side) -> be.sideConfig().energyView(side),
                ModBlockEntities.FOUNDATION_MACHINE.get());
        FabricFluidLookup.FLUID.registerForBlockEntity((be, side) -> be.sideConfig().fluidView(side),
                ModBlockEntities.FOUNDATION_MACHINE.get());
    }
}
