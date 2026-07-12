package za.co.neroland.neroagriculture.automation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.core.BlockPos;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class AutomationPolicyTest {
    @AfterEach
    void cleanup() {
        AutomationPolicy.GUARDS.clear();
    }

    @Test
    void standaloneWithNoGuardsAllowsEditing() {
        assertTrue(AutomationPolicy.mayEdit(null, BlockPos.ZERO, null));
    }

    @Test
    void anyDenyingGuardFailsClosed() {
        AutomationPolicy.GUARDS.add((level, pos, owner) -> false); // a guard that never denies
        assertTrue(AutomationPolicy.mayEdit(null, BlockPos.ZERO, null));
        AutomationPolicy.GUARDS.add((level, pos, owner) -> true); // a guard that denies
        assertFalse(AutomationPolicy.mayEdit(null, BlockPos.ZERO, null));
    }
}
