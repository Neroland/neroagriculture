package za.co.neroland.neroagriculture.menu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.world.inventory.SimpleContainerData;

import org.junit.jupiter.api.Test;

/**
 * Locks the exact-value sync behind the gauge tooltips: a value split into two 15-bit halves survives
 * the signed-short slot sync, clamps outside 0..2^30-1, and {@link GaugeData#withRaw} appends the pairs
 * after the existing fraction slots without moving them.
 */
class GaugeDataTest {

    @Test
    void splitRoundTrips() {
        for (long v : new long[] {0L, 1L, 32_767L, 32_768L, 72_000L, 1_000_000L, 10_000_000L, GaugeData.MAX_SPLIT}) {
            int low = GaugeData.low(v);
            int high = GaugeData.high(v);
            assertTrue(low >= 0 && low <= Short.MAX_VALUE);
            assertTrue(high >= 0 && high <= Short.MAX_VALUE);
            // The wire carries shorts: the halves must come back unchanged.
            assertEquals(v, GaugeData.join((short) low, (short) high));
        }
    }

    @Test
    void splitClamps() {
        assertEquals(0L, GaugeData.join(GaugeData.low(-5L), GaugeData.high(-5L)));
        assertEquals(GaugeData.MAX_SPLIT, GaugeData.join(GaugeData.low(Long.MAX_VALUE), GaugeData.high(Long.MAX_VALUE)));
    }

    @Test
    void withRawAppendsPairs() {
        SimpleContainerData base = new SimpleContainerData(3);
        base.set(0, 7);
        base.set(2, 500);
        var data = GaugeData.withRaw(base, () -> 1_234_567L, () -> 10_000_000L);
        assertEquals(3 + GaugeData.splitSlots(2), data.getCount());
        assertEquals(7, data.get(0));
        assertEquals(500, data.get(2));
        assertEquals(1_234_567L, GaugeData.join(data.get(3), data.get(4)));
        assertEquals(10_000_000L, GaugeData.join(data.get(5), data.get(6)));
        assertEquals(0, data.get(99));
    }
}
