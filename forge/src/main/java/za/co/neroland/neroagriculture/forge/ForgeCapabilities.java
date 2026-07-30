package za.co.neroland.neroagriculture.forge;

import java.util.EnumMap;

import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
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
 * Forge capability exposure for EVERY NeroAgriculture machine (energy/fluid when the machine has that
 * channel; items when it is a worldly container) — so Core's battery push, hoppers and pipes reach all
 * machines, not just the fabrication machines and grow beds. The exposed energy/fluid handlers are thin
 * per-side views that resolve the machine's CURRENT side-config gate ({@code energyView(side)} /
 * {@code fluidView(side)}) on every operation — mirroring the per-query Fabric/NeoForge lookups — so a
 * disabled face no-ops and later side-config changes apply immediately, without needing a capability
 * invalidation callback from Core. {@link LazyOptional#empty()} is returned only for channels the
 * machine can never have.
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

    /** True when the machine declares the channel at all (any face, or the ungated null-side view). */
    private static boolean hasEnergyChannel(AbstractMachineBlockEntity machine) {
        if (machine.sideConfig().energyView(null) != null) return true;
        for (Direction side : Direction.values()) {
            if (machine.sideConfig().energyView(side) != null) return true;
        }
        return false;
    }

    private static boolean hasFluidChannel(AbstractMachineBlockEntity machine) {
        if (machine.sideConfig().fluidView(null) != null) return true;
        for (Direction side : Direction.values()) {
            if (machine.sideConfig().fluidView(side) != null) return true;
        }
        return false;
    }

    /** Energy view that re-resolves the side-config gate per call; no-ops when the face is disabled. */
    private record SideEnergyView(AbstractMachineBlockEntity machine, @Nullable Direction side)
            implements NeroEnergyStorage {
        @Nullable private NeroEnergyStorage view() { return machine.sideConfig().energyView(side); }
        @Override public long getAmount() { NeroEnergyStorage view = view(); return view == null ? 0 : view.getAmount(); }
        @Override public long getCapacity() { NeroEnergyStorage view = view(); return view == null ? 0 : view.getCapacity(); }
        @Override public long insert(long maxAmount, boolean simulate) {
            NeroEnergyStorage view = view();
            return view == null ? 0 : view.insert(maxAmount, simulate);
        }
        @Override public long extract(long maxAmount, boolean simulate) {
            NeroEnergyStorage view = view();
            return view == null ? 0 : view.extract(maxAmount, simulate);
        }
    }

    /** Fluid view that re-resolves the side-config gate per call; no-ops when the face is disabled. */
    private record SideFluidView(AbstractMachineBlockEntity machine, @Nullable Direction side)
            implements NeroFluidStorage {
        @Nullable private NeroFluidStorage view() { return machine.sideConfig().fluidView(side); }
        @Override public Fluid getFluid() { NeroFluidStorage view = view(); return view == null ? Fluids.EMPTY : view.getFluid(); }
        @Override public long getAmount() { NeroFluidStorage view = view(); return view == null ? 0 : view.getAmount(); }
        @Override public long getCapacity() { NeroFluidStorage view = view(); return view == null ? 0 : view.getCapacity(); }
        @Override public long fill(Fluid fluid, long amount, boolean simulate) {
            NeroFluidStorage view = view();
            return view == null ? 0 : view.fill(fluid, amount, simulate);
        }
        @Override public long drain(long amount, boolean simulate) {
            NeroFluidStorage view = view();
            return view == null ? 0 : view.drain(amount, simulate);
        }
    }

    private static final class MachineCaps implements ICapabilityProvider {
        private final AbstractMachineBlockEntity machine;
        private final boolean hasEnergy;
        private final boolean hasFluid;
        private final LazyOptional<NeroEnergyStorage> unsidedEnergy;
        private final LazyOptional<NeroFluidStorage> unsidedFluid;
        private final LazyOptional<IItemHandler> unsided;
        private final EnumMap<Direction, LazyOptional<NeroEnergyStorage>> sidedEnergy = new EnumMap<>(Direction.class);
        private final EnumMap<Direction, LazyOptional<NeroFluidStorage>> sidedFluid = new EnumMap<>(Direction.class);
        private final EnumMap<Direction, LazyOptional<IItemHandler>> sided = new EnumMap<>(Direction.class);

        MachineCaps(AbstractMachineBlockEntity machine) {
            this.machine = machine;
            this.hasEnergy = hasEnergyChannel(machine);
            this.hasFluid = hasFluidChannel(machine);
            this.unsidedEnergy = LazyOptional.of(() -> new SideEnergyView(machine, null));
            this.unsidedFluid = LazyOptional.of(() -> new SideFluidView(machine, null));
            this.unsided = machine instanceof WorldlyContainer container
                    ? LazyOptional.of(() -> new InvWrapper(container)) : LazyOptional.empty();
        }

        @Override public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable Direction side) {
            if (capability == ForgeEnergyLookup.ENERGY) {
                if (!hasEnergy) return LazyOptional.empty();
                return (side == null ? unsidedEnergy : sidedEnergy.computeIfAbsent(side,
                        direction -> LazyOptional.of(() -> new SideEnergyView(machine, direction)))).cast();
            }
            if (capability == ForgeFluidLookup.FLUID) {
                if (!hasFluid) return LazyOptional.empty();
                return (side == null ? unsidedFluid : sidedFluid.computeIfAbsent(side,
                        direction -> LazyOptional.of(() -> new SideFluidView(machine, direction)))).cast();
            }
            if (capability == net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER
                    && machine instanceof WorldlyContainer container) {
                return (side == null ? unsided : sided.computeIfAbsent(side,
                        direction -> LazyOptional.of(() -> new SidedInvWrapper(container, direction)))).cast();
            }
            return LazyOptional.empty();
        }

        void invalidate() {
            unsidedEnergy.invalidate();
            unsidedFluid.invalidate();
            unsided.invalidate();
            sidedEnergy.values().forEach(LazyOptional::invalidate);
            sidedFluid.values().forEach(LazyOptional::invalidate);
            sided.values().forEach(LazyOptional::invalidate);
        }
    }
}
