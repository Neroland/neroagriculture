package za.co.neroland.neroagriculture.machine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import za.co.neroland.nerolandcore.sideconfig.Channel;
import za.co.neroland.nerolandcore.sideconfig.RelativeFace;
import za.co.neroland.nerolandcore.sideconfig.SideConfig;
import za.co.neroland.nerolandcore.sideconfig.SideMode;
import za.co.neroland.nerolandcore.sideconfig.SidePreset;
import za.co.neroland.nerolandcore.sideconfig.SlotGroup;

/**
 * Locks both halves of the dead-faces fix. Every machine must declare
 * {@code SidePreset.PROCESSOR} so power is accepted on all six faces — Core's adjacent
 * push always queries the energy capability with a non-null face, and a DISABLED face
 * hands back no capability at all, so an unseeded machine is invisible to a neighbouring
 * Battery. The one-time reseed then has to fire for pre-fix NBT and never again. Pure JVM.
 */
class SideConfigMigrationTest {
    @Test
    void processorPresetAcceptsPowerOnEveryFace() {
        SideConfig config = foundationMachine();
        for (RelativeFace face : RelativeFace.VALUES) {
            assertEquals(SideMode.INPUT, config.mode(Channel.ENERGY, face), "power face " + face);
        }
        // Material channels: out of the bottom, in everywhere else.
        assertEquals(SideMode.OUTPUT, config.mode(Channel.ITEM, RelativeFace.BOTTOM));
        assertEquals(SideMode.INPUT, config.mode(Channel.ITEM, RelativeFace.TOP));
        assertEquals(SideMode.INPUT, config.mode(Channel.ITEM, RelativeFace.FRONT));
    }

    @Test
    void forbiddingPowerOutputAndIoStillLeavesEveryFaceAcceptingPower() {
        // The Greenhouse Controller's declaration: power in only, never out, never both.
        // PROCESSOR wants INPUT, which survives that clamping untouched.
        SideConfig config = SideConfig.builder().channel(Channel.ENERGY).channel(Channel.FLUID)
                .defaultPreset(SidePreset.PROCESSOR)
                .allow(Channel.ENERGY, SideMode.OUTPUT, false).allow(Channel.ENERGY, SideMode.IO, false)
                .allow(Channel.FLUID, SideMode.OUTPUT, false).allow(Channel.FLUID, SideMode.IO, false).build();
        for (RelativeFace face : RelativeFace.VALUES) {
            assertEquals(SideMode.INPUT, config.mode(Channel.ENERGY, face), "power face " + face);
        }
        // The fluid bottom face wanted OUTPUT; with both OUTPUT and IO forbidden it clamps to DISABLED.
        assertEquals(SideMode.DISABLED, config.mode(Channel.FLUID, RelativeFace.BOTTOM));
        assertEquals(SideMode.INPUT, config.mode(Channel.FLUID, RelativeFace.TOP));
    }

    @Test
    void reseedFiresOnceForPreFixSavesAndIsIdempotent() {
        SideConfig config = foundationMachine();
        // A machine saved before the fix: every face of every channel DISABLED.
        for (RelativeFace face : RelativeFace.VALUES) {
            config.setMode(Channel.ENERGY, face, SideMode.DISABLED);
        }
        assertEquals(SideMode.DISABLED, config.mode(Channel.ENERGY, RelativeFace.TOP));

        assertTrue(SideConfigMigration.needsReseed(0));
        assertTrue(SideConfigMigration.apply(config, 0));
        assertEquals(SideMode.INPUT, config.mode(Channel.ENERGY, RelativeFace.TOP));

        // Re-running the migration re-applies the same preset: no drift.
        assertTrue(SideConfigMigration.apply(config, 0));
        assertEquals(SideMode.INPUT, config.mode(Channel.ENERGY, RelativeFace.TOP));
        assertEquals(SideMode.OUTPUT, config.mode(Channel.ITEM, RelativeFace.BOTTOM));
    }

    @Test
    void reseedNeverClobbersChoicesMadeAfterTheFix() {
        SideConfig config = foundationMachine();
        // A deliberate post-fix choice: the player disables the front power face.
        config.setMode(Channel.ENERGY, RelativeFace.FRONT, SideMode.DISABLED);

        assertFalse(SideConfigMigration.needsReseed(SideConfigMigration.VERSION));
        assertFalse(SideConfigMigration.apply(config, SideConfigMigration.VERSION));
        assertEquals(SideMode.DISABLED, config.mode(Channel.ENERGY, RelativeFace.FRONT));
        // A save from a future version is left alone too.
        assertFalse(SideConfigMigration.apply(config, SideConfigMigration.VERSION + 1));
        assertEquals(SideMode.DISABLED, config.mode(Channel.ENERGY, RelativeFace.FRONT));
    }

    /** The Foundation Machine's declared surface, mirrored here without the game. */
    private static SideConfig foundationMachine() {
        return SideConfig.builder()
                .channel(Channel.ITEM, SlotGroup.of("input", 0, 1, 2), SlotGroup.of("output", 3, 4))
                .channel(Channel.FLUID).channel(Channel.ENERGY)
                .defaultPreset(SidePreset.PROCESSOR)
                .allow(Channel.ENERGY, SideMode.OUTPUT, false).build();
    }
}
