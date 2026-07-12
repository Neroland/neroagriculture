package za.co.neroland.neroagriculture.forge;

import java.util.EnumMap;

import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.InvWrapper;
import net.minecraftforge.items.wrapper.SidedInvWrapper;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.neroagriculture.NeroAgricultureCommon;
import za.co.neroland.neroagriculture.machine.FoundationMachineBlockEntity;
import za.co.neroland.neroagriculture.crop.GrowBedBlockEntity;
import za.co.neroland.nerolandcore.energy.NeroEnergyStorage;
import za.co.neroland.nerolandcore.fluid.NeroFluidStorage;
import za.co.neroland.nerolandcore.platform.ForgeEnergyLookup;
import za.co.neroland.nerolandcore.platform.ForgeFluidLookup;

public final class ForgeCapabilities {
    private static final Identifier ID = Identifier.fromNamespaceAndPath(NeroAgricultureCommon.MOD_ID, "machine_caps");
    private ForgeCapabilities() { }
    public static void register() { AttachCapabilitiesEvent.BlockEntities.BUS.addListener(ForgeCapabilities::attach); }
    private static void attach(AttachCapabilitiesEvent.BlockEntities event) {
        if (event.getObject() instanceof FoundationMachineBlockEntity machine) {
            MachineCaps caps = new MachineCaps(machine);
            event.addCapability(ID, caps);
            event.addListener(caps::invalidate);
        } else if (event.getObject() instanceof GrowBedBlockEntity bed) {
            BedCaps caps = new BedCaps(bed);
            event.addCapability(Identifier.fromNamespaceAndPath(NeroAgricultureCommon.MOD_ID, "grow_bed_caps"), caps);
            event.addListener(caps::invalidate);
        }
    }
    private static final class MachineCaps implements ICapabilityProvider {
        private final FoundationMachineBlockEntity machine;
        private final LazyOptional<NeroEnergyStorage> energy;
        private final LazyOptional<NeroFluidStorage> fluid;
        private final LazyOptional<IItemHandler> unsided;
        private final EnumMap<Direction, LazyOptional<IItemHandler>> sided = new EnumMap<>(Direction.class);
        MachineCaps(FoundationMachineBlockEntity machine) {
            this.machine = machine;
            this.energy = LazyOptional.of(machine::getEnergy);
            this.fluid = LazyOptional.of(machine::getFluid);
            this.unsided = LazyOptional.of(() -> new InvWrapper(machine));
        }
        @Override public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable Direction side) {
            if (capability == ForgeEnergyLookup.ENERGY) return energy.cast();
            if (capability == ForgeFluidLookup.FLUID) return fluid.cast();
            if (capability == net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER) return (side == null ? unsided : sided.computeIfAbsent(side,
                    direction -> LazyOptional.of(() -> new SidedInvWrapper(machine, direction)))).cast();
            return LazyOptional.empty();
        }
        void invalidate() { energy.invalidate(); fluid.invalidate(); unsided.invalidate(); sided.values().forEach(LazyOptional::invalidate); }
    }
    private static final class BedCaps implements ICapabilityProvider {
        private final LazyOptional<NeroEnergyStorage> energy;
        private final LazyOptional<NeroFluidStorage> fluid;
        BedCaps(GrowBedBlockEntity bed) {
            this.energy = LazyOptional.of(bed::getEnergy);
            this.fluid = LazyOptional.of(bed::getFluid);
        }
        @Override public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable Direction side) {
            if (capability == ForgeEnergyLookup.ENERGY) return energy.cast();
            if (capability == ForgeFluidLookup.FLUID) return fluid.cast();
            return LazyOptional.empty();
        }
        void invalidate() { energy.invalidate(); fluid.invalidate(); }
    }
}
