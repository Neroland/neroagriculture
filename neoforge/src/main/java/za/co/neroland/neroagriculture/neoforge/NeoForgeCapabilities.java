package za.co.neroland.neroagriculture.neoforge;

import java.util.List;

import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.transfer.item.VanillaContainerWrapper;
import net.neoforged.neoforge.transfer.item.WorldlyContainerWrapper;

import za.co.neroland.neroagriculture.registry.ModBlockEntities;
import za.co.neroland.nerolandcore.machine.AbstractMachineBlockEntity;
import za.co.neroland.nerolandcore.platform.NeoForgeEnergyLookup;
import za.co.neroland.nerolandcore.platform.NeoForgeFluidLookup;

/**
 * NeoForge capability exposure for EVERY NeroAgriculture machine. Energy and fluid go through the
 * gated side-config views (null-safe when a machine has no such channel) and items through the
 * worldly-container wrapper — so Core's battery push, hoppers and pipes reach all machines, not just
 * the fabrication machines and grow beds.
 */
public final class NeoForgeCapabilities {
    private NeoForgeCapabilities() { }
    public static void register(IEventBus bus) { bus.addListener(NeoForgeCapabilities::onRegister); }

    private static void onRegister(RegisterCapabilitiesEvent event) {
        List<BlockEntityType<? extends AbstractMachineBlockEntity>> machines = List.of(
                ModBlockEntities.FOUNDATION_MACHINE.get(),
                ModBlockEntities.GROW_BED.get(),
                ModBlockEntities.GENETICS_STATION.get(),
                ModBlockEntities.AREA_MACHINE.get(),
                ModBlockEntities.CROP_TOWER_CONTROLLER.get(),
                ModBlockEntities.GREENHOUSE_CONTROLLER.get(),
                ModBlockEntities.BIOREACTOR.get(),
                ModBlockEntities.BIOFUEL_CONVERTER.get(),
                ModBlockEntities.FERTILISER_PROCESSOR.get(),
                ModBlockEntities.POLLINATION_BEACON.get(),
                ModBlockEntities.TERRAFORMING_CONTROLLER.get());
        for (BlockEntityType<? extends AbstractMachineBlockEntity> type : machines) {
            event.registerBlockEntity(NeoForgeEnergyLookup.ENERGY, type,
                    (be, side) -> be.sideConfig().energyView(side));
            event.registerBlockEntity(NeoForgeFluidLookup.FLUID, type,
                    (be, side) -> be.sideConfig().fluidView(side));
            event.registerBlockEntity(Capabilities.Item.BLOCK, type, (be, side) -> {
                if (!(be instanceof WorldlyContainer container)) return null;
                return side == null ? VanillaContainerWrapper.of(container)
                        : new WorldlyContainerWrapper(container, side);
            });
        }
    }
}
