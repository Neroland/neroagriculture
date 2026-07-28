package za.co.neroland.neroagriculture.fabric;

import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ContainerStorage;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.level.block.entity.BlockEntityType;

import za.co.neroland.neroagriculture.registry.ModBlockEntities;
import za.co.neroland.nerolandcore.machine.AbstractMachineBlockEntity;
import za.co.neroland.nerolandcore.platform.FabricEnergyLookup;
import za.co.neroland.nerolandcore.platform.FabricFluidLookup;

/**
 * Fabric exposure of the shared Core item/fluid/energy surfaces for EVERY NeroAgriculture machine —
 * so Core's battery push, hoppers and pipes reach all machines, not just the fabrication machines and
 * grow beds. Energy/fluid go through the gated side-config views (null-safe when absent). The machine
 * list is the canonical {@link ModBlockEntities#machineTypes()}.
 */
public final class FabricCapabilities {
    private FabricCapabilities() { }

    public static void register() {
        for (BlockEntityType<? extends AbstractMachineBlockEntity> type : ModBlockEntities.machineTypes()) {
            FabricEnergyLookup.ENERGY.registerForBlockEntity((be, side) -> be.sideConfig().energyView(side), type);
            FabricFluidLookup.FLUID.registerForBlockEntity((be, side) -> be.sideConfig().fluidView(side), type);
            ItemStorage.SIDED.registerForBlockEntity((be, side) ->
                    be instanceof WorldlyContainer container ? ContainerStorage.of(container, side) : null, type);
        }
    }
}
