package za.co.neroland.neroagriculture.content;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;

class FragmentChargeTest {
    @Test void saveRoundTripPreservesTierAndRejectsUnknownVersion() {
        FragmentCharge charge = FragmentCharge.of(FragmentTier.VOIDITE);
        var json = FragmentCharge.CODEC.encodeStart(JsonOps.INSTANCE, charge).getOrThrow();
        assertEquals(charge, FragmentCharge.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow());
        assertThrows(IllegalArgumentException.class,
                () -> new FragmentCharge(FragmentCharge.CURRENT_VERSION + 1, FragmentTier.TERRITE));
    }
}
