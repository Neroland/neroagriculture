package za.co.neroland.neroagriculture.compat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import net.minecraft.resources.Identifier;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import za.co.neroland.neroagriculture.api.AgricultureApi;
import za.co.neroland.neroagriculture.cycle.CycleApi;
import za.co.neroland.neroagriculture.cycle.CycleModifier;
import za.co.neroland.neroagriculture.environment.EnvironmentApi;
import za.co.neroland.neroagriculture.environment.EnvironmentProfile;
import za.co.neroland.neroagriculture.environment.Temperature;

class CompatContractsTest {
    @AfterEach
    void cleanup() {
        AgricultureApi.BIOFUEL.clear();
        CycleApi.PROVIDERS.clear();
        EnvironmentApi.PROVIDERS.clear();
    }

    @Test
    void everySeamIsDormantByDefaultSoTheModRunsStandalone() {
        assertFalse(CompatContracts.hasBiofuelConsumer(), "no biofuel consumer without an integration");
        assertFalse(CompatContracts.hasEnvironmentProvider(), "no environment provider without an integration");
        assertEquals(CycleModifier.IDENTITY, CompatContracts.externalCycle(Identifier.parse("minecraft:overworld"), 0),
                "with no cycle provider the external contribution is exactly 1.0");
    }

    @Test
    void biofuelConsumersCanRegisterAndUnregister() {
        AgricultureApi.BiofuelConsumer consumer = (offer, simulate) -> offer.amountMb();
        CompatContracts.registerBiofuelConsumer(consumer);
        assertTrue(CompatContracts.hasBiofuelConsumer());
        CompatContracts.removeBiofuelConsumer(consumer);
        assertFalse(CompatContracts.hasBiofuelConsumer());
    }

    @Test
    void aRegisteredCycleProviderContributesAndUnregistersCleanly() {
        CycleApi.Provider provider = (dimension, time) -> new CycleModifier(2.0F, 2.0F, 1.0F);
        CompatContracts.registerCycleProvider(provider);
        assertEquals(2.0F, CompatContracts.externalCycle(Identifier.parse("minecraft:overworld"), 0).growth());
        CompatContracts.removeCycleProvider(provider);
        assertEquals(CycleModifier.IDENTITY, CompatContracts.externalCycle(Identifier.parse("minecraft:overworld"), 0));
    }

    @Test
    void anEnvironmentProviderOverridesTheLocalFallback() {
        EnvironmentProfile hostile = new EnvironmentProfile(Temperature.HOT, false, false);
        assertEquals(EnvironmentProfile.HABITABLE,
                CompatContracts.environmentOrDefault((level, pos) -> Optional.empty(), null, null),
                "with no provider, the fallback default is habitable");
        CompatContracts.registerEnvironmentProvider((level, pos) -> Optional.of(hostile));
        assertEquals(hostile, CompatContracts.environmentOrDefault((level, pos) -> Optional.empty(), null, null),
                "a registered provider takes precedence");
    }
}
