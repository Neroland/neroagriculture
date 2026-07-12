package za.co.neroland.neroagriculture.crop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.world.level.material.Fluid;

import org.junit.jupiter.api.Test;

import za.co.neroland.nerolandcore.energy.NeroEnergyStorage;
import za.co.neroland.nerolandcore.fluid.NeroFluidStorage;

class GrowBedResourcesTest {
    @Test
    void transactionConsumesBothOrNeither() {
        MutableEnergy energy = new MutableEnergy(100);
        MutableFluid fluid = new MutableFluid(100);
        assertTrue(GrowBedResources.consume(energy, fluid, null, 40, 25));
        assertEquals(60, energy.getAmount());
        assertEquals(75, fluid.getAmount());

        assertFalse(GrowBedResources.consume(energy, fluid, null, 80, 25));
        assertEquals(60, energy.getAmount());
        assertEquals(75, fluid.getAmount());
        assertFalse(GrowBedResources.consume(energy, fluid, null, 40, 80));
        assertEquals(60, energy.getAmount());
        assertEquals(75, fluid.getAmount());
    }

    private static final class MutableEnergy implements NeroEnergyStorage {
        private long amount;
        private MutableEnergy(long amount) { this.amount = amount; }
        @Override public long getAmount() { return amount; }
        @Override public long getCapacity() { return 1000; }
        @Override public long insert(long requested, boolean simulate) { return 0; }
        @Override public long extract(long requested, boolean simulate) {
            long extracted = Math.min(requested, amount);
            if (!simulate) amount -= extracted;
            return extracted;
        }
    }

    private static final class MutableFluid implements NeroFluidStorage {
        private long amount;
        private MutableFluid(long amount) { this.amount = amount; }
        @Override public Fluid getFluid() { return null; }
        @Override public long getAmount() { return amount; }
        @Override public long getCapacity() { return 1000; }
        @Override public long fill(Fluid fluid, long requested, boolean simulate) { return 0; }
        @Override public long drain(long requested, boolean simulate) {
            long drained = Math.min(requested, amount);
            if (!simulate) amount -= drained;
            return drained;
        }
    }
}
