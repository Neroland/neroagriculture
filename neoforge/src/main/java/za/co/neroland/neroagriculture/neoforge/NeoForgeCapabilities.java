package za.co.neroland.neroagriculture.neoforge;

//? if >=1.20.3 {
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
//? if >=1.21.9 {
import net.neoforged.neoforge.transfer.item.VanillaContainerWrapper;
import net.neoforged.neoforge.transfer.item.WorldlyContainerWrapper;
//?}

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
            //? if >=1.21.9 {
            event.registerBlockEntity(Capabilities.Item.BLOCK, type, (be, side) -> {
                if (!(be instanceof WorldlyContainer container)) return null;
                return side == null ? VanillaContainerWrapper.of(container)
                        : new WorldlyContainerWrapper(container, side);
            });
            //?} else {
            /*// Before 1.21.9 NeoForge has no transfer API: the item surface is IItemHandler.
            event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, type, (be, side) -> {
                if (!(be instanceof WorldlyContainer container)) return null;
                return side == null ? new net.neoforged.neoforge.items.wrapper.InvWrapper(container)
                        : new net.neoforged.neoforge.items.wrapper.SidedInvWrapper(container, side);
            });
            *///?}
        }
    }
}
//?} else {
/*import java.util.EnumMap;

import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.common.capabilities.Capability;
import net.neoforged.neoforge.common.capabilities.ICapabilityProvider;
import net.neoforged.neoforge.common.util.LazyOptional;
import net.neoforged.neoforge.event.AttachCapabilitiesEvent;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.wrapper.InvWrapper;
import net.neoforged.neoforge.items.wrapper.SidedInvWrapper;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.neroagriculture.NeroAgricultureCommon;
import za.co.neroland.nerolandcore.energy.NeroEnergyStorage;
import za.co.neroland.nerolandcore.fluid.NeroFluidStorage;
import za.co.neroland.nerolandcore.machine.AbstractMachineBlockEntity;
import za.co.neroland.nerolandcore.platform.NeoForgeEnergyLookup;
import za.co.neroland.nerolandcore.platform.NeoForgeFluidLookup;

// NeoForge 20.2 capability exposure (Forge-style attach, before the 20.3 capability rework) for EVERY NeroAgriculture machine (energy/fluid when the machine has that
// channel; items when it is a worldly container) — so Core's battery push, hoppers and pipes reach all
// machines, not just the fabrication machines and grow beds. The exposed energy/fluid handlers are thin
// per-side views that resolve the machine's CURRENT side-config gate ({@code energyView(side)} /
// {@code fluidView(side)}) on every operation — mirroring the per-query Fabric/NeoForge lookups — so a
// disabled face no-ops and later side-config changes apply immediately, without needing a capability
// invalidation callback from Core. {@link LazyOptional#empty()} is returned only for channels the
// machine can never have.
public final class NeoForgeCapabilities {
    private static final Identifier ID = Identifier.fromNamespaceAndPath(NeroAgricultureCommon.MOD_ID, "machine_caps");
    private NeoForgeCapabilities() { }
    public static void register(net.neoforged.bus.api.IEventBus modEventBus) {
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addGenericListener(
                net.minecraft.world.level.block.entity.BlockEntity.class, NeoForgeCapabilities::attach);
    }

    private static void attach(AttachCapabilitiesEvent<net.minecraft.world.level.block.entity.BlockEntity> event) {
        if (event.getObject() instanceof AbstractMachineBlockEntity machine
                && NeroAgricultureCommon.MOD_ID.equals(
                        net.minecraft.core.registries.BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(machine.getType()).getNamespace())) {
            MachineCaps caps = new MachineCaps(machine);
            event.addCapability(ID, caps);
            event.addListener(caps::invalidate);
        }
    }

    // True when the machine declares the channel at all (any face, or the ungated null-side view).
    private static boolean hasEnergyChannel(AbstractMachineBlockEntity machine) {
        var config = machine.sideConfig();
        if (config == null) return false;
        if (config.energyView(null) != null) return true;
        for (Direction side : Direction.values()) {
            if (config.energyView(side) != null) return true;
        }
        return false;
    }

    private static boolean hasFluidChannel(AbstractMachineBlockEntity machine) {
        var config = machine.sideConfig();
        if (config == null) return false;
        if (config.fluidView(null) != null) return true;
        for (Direction side : Direction.values()) {
            if (config.fluidView(side) != null) return true;
        }
        return false;
    }

    // Energy view that re-resolves the side-config gate per call; no-ops when the face is disabled.
    private record SideEnergyView(AbstractMachineBlockEntity machine, @Nullable Direction side)
            implements NeroEnergyStorage {
        @Nullable private NeroEnergyStorage view() {
            var config = machine.sideConfig();
            return config == null ? null : config.energyView(side);
        }
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

    // Fluid view that re-resolves the side-config gate per call; no-ops when the face is disabled.
    private record SideFluidView(AbstractMachineBlockEntity machine, @Nullable Direction side)
            implements NeroFluidStorage {
        @Nullable private NeroFluidStorage view() {
            var config = machine.sideConfig();
            return config == null ? null : config.fluidView(side);
        }
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
        private final LazyOptional<NeroEnergyStorage> unsidedEnergy;
        private final LazyOptional<NeroFluidStorage> unsidedFluid;
        private final LazyOptional<IItemHandler> unsided;
        private final EnumMap<Direction, LazyOptional<NeroEnergyStorage>> sidedEnergy = new EnumMap<>(Direction.class);
        private final EnumMap<Direction, LazyOptional<NeroFluidStorage>> sidedFluid = new EnumMap<>(Direction.class);
        private final EnumMap<Direction, LazyOptional<IItemHandler>> sided = new EnumMap<>(Direction.class);

        MachineCaps(AbstractMachineBlockEntity machine) {
            this.machine = machine;
            this.unsidedEnergy = LazyOptional.of(() -> new SideEnergyView(machine, null));
            this.unsidedFluid = LazyOptional.of(() -> new SideFluidView(machine, null));
            this.unsided = machine instanceof WorldlyContainer container
                    ? LazyOptional.of(() -> new InvWrapper(container)) : LazyOptional.empty();
        }

        @Override public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable Direction side) {
            if (capability == NeoForgeEnergyLookup.ENERGY) {
                if (!hasEnergyChannel(machine)) return LazyOptional.empty();
                return (side == null ? unsidedEnergy : sidedEnergy.computeIfAbsent(side,
                        direction -> LazyOptional.of(() -> new SideEnergyView(machine, direction)))).cast();
            }
            if (capability == NeoForgeFluidLookup.FLUID) {
                if (!hasFluidChannel(machine)) return LazyOptional.empty();
                return (side == null ? unsidedFluid : sidedFluid.computeIfAbsent(side,
                        direction -> LazyOptional.of(() -> new SideFluidView(machine, direction)))).cast();
            }
            if (capability == net.neoforged.neoforge.common.capabilities.Capabilities.ITEM_HANDLER
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
*///?}
