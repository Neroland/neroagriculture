package za.co.neroland.neroagriculture.forge;

import java.util.EnumMap;

import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.WorldlyContainer;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.InvWrapper;
import net.minecraftforge.items.wrapper.SidedInvWrapper;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.neroagriculture.NeroAgricultureCommon;
import za.co.neroland.nerolandcore.energy.NeroEnergyStorage;
import za.co.neroland.nerolandcore.fluid.NeroFluidStorage;
import za.co.neroland.nerolandcore.machine.AbstractMachineBlockEntity;
import za.co.neroland.nerolandcore.platform.ForgeEnergyLookup;
import za.co.neroland.nerolandcore.platform.ForgeFluidLookup;

/**
 * Forge capability exposure for EVERY NeroAgriculture machine (energy always; fluid when the machine
 * has a fluid channel; items when it is a worldly container) — so Core's battery push, hoppers and
 * pipes reach all machines, not just the fabrication machines and grow beds.
 */
public final class ForgeCapabilities {
    private static final Identifier ID = Identifier.fromNamespaceAndPath(NeroAgricultureCommon.MOD_ID, "machine_caps");
    private ForgeCapabilities() { }
    public static void register() { AttachCapabilitiesEvent.BlockEntities.BUS.addListener(ForgeCapabilities::attach); }

    private static void attach(AttachCapabilitiesEvent.BlockEntities event) {
        if (event.getObject() instanceof AbstractMachineBlockEntity machine
                && NeroAgricultureCommon.MOD_ID.equals(
                        net.minecraft.core.registries.BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(machine.getType()).getNamespace())) {
            MachineCaps caps = new MachineCaps(machine);
            event.addCapability(ID, caps);
            event.addListener(caps::invalidate);
        }
    }

    private static final class MachineCaps implements ICapabilityProvider {
        private final AbstractMachineBlockEntity machine;
        private final LazyOptional<NeroEnergyStorage> energy;
        private final LazyOptional<NeroFluidStorage> fluid;
        private final LazyOptional<IItemHandler> unsided;
        private final EnumMap<Direction, LazyOptional<IItemHandler>> sided = new EnumMap<>(Direction.class);

        MachineCaps(AbstractMachineBlockEntity machine) {
            this.machine = machine;
            this.energy = LazyOptional.of(machine::getEnergy);
            NeroFluidStorage fluidDelegate = machine.sideConfig().fluidView(null);
            this.fluid = fluidDelegate == null ? LazyOptional.empty() : LazyOptional.of(() -> fluidDelegate);
            this.unsided = machine instanceof WorldlyContainer container
                    ? LazyOptional.of(() -> new InvWrapper(container)) : LazyOptional.empty();
        }

        @Override public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable Direction side) {
            if (capability == ForgeEnergyLookup.ENERGY) return energy.cast();
            if (capability == ForgeFluidLookup.FLUID) return fluid.cast();
            if (capability == net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER
                    && machine instanceof WorldlyContainer container) {
                return (side == null ? unsided : sided.computeIfAbsent(side,
                        direction -> LazyOptional.of(() -> new SidedInvWrapper(container, direction)))).cast();
            }
            return LazyOptional.empty();
        }

        void invalidate() {
            energy.invalidate();
            fluid.invalidate();
            unsided.invalidate();
            sided.values().forEach(LazyOptional::invalidate);
        }
    }
}
