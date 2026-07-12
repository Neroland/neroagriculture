package za.co.neroland.neroagriculture.content;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;

class EssenceChargeTest {
    @Test void saveRoundTripPreservesTierAndRejectsUnknownVersion() {
        EssenceCharge charge = EssenceCharge.of(EssenceFamily.DEEPVOID);
        var json = EssenceCharge.CODEC.encodeStart(JsonOps.INSTANCE, charge).getOrThrow();
        assertEquals(charge, EssenceCharge.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow());
        assertThrows(IllegalArgumentException.class,
                () -> new EssenceCharge(EssenceCharge.CURRENT_VERSION + 1, EssenceFamily.TERRAN));
    }
}
