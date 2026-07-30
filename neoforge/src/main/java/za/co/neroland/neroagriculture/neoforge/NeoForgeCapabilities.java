package za.co.neroland.neroagriculture.neoforge;

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
 * the fabrication machines and grow beds. The machine list is the canonical
 * {@link ModBlockEntities#machineTypes()}.
 */
public final class NeoForgeCapabilities {
    private NeoForgeCapabilities() { }
    public static void register(IEventBus bus) { bus.addListener(NeoForgeCapabilities::onRegister); }

    private static void onRegister(RegisterCapabilitiesEvent event) {
        for (BlockEntityType<? extends AbstractMachineBlockEntity> type : ModBlockEntities.machineTypes()) {
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
