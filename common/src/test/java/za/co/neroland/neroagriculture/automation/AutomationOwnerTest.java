package za.co.neroland.neroagriculture.automation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import org.junit.jupiter.api.Test;

class AutomationOwnerTest {
    @Test
    void openingAnOwnerlessMachineCanClaimItOnlyWithTrackingEnabled() {
        assertTrue(AutomationOwner.canClaim(null, true));
        assertFalse(AutomationOwner.canClaim(null, false));
    }

    @Test
    void openingAnOwnedMachineNeverReplacesItsOwner() {
        UUID owner = UUID.randomUUID();
        assertFalse(AutomationOwner.canClaim(owner, true));
        assertFalse(AutomationOwner.canClaim(owner, false));
    }

    @Test
    void progressionResolvesOnlyTheRecordedOwnerAndFailsClosedOffline() {
        UUID owner = UUID.randomUUID();
        UUID bystander = UUID.randomUUID();
        assertEquals("owner", AutomationOwner.onlineOwner(owner,
                id -> id.equals(owner) ? "owner" : "bystander"));
        assertNull(AutomationOwner.onlineOwner(owner, id -> id.equals(bystander) ? "bystander" : null));
        assertNull(AutomationOwner.onlineOwner(null, id -> { throw new AssertionError("ownerless lookup"); }));
    }
}
