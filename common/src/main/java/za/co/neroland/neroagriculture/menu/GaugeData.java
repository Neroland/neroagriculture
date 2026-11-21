package za.co.neroland.neroagriculture.menu;

import java.util.function.LongSupplier;

import net.minecraft.world.inventory.ContainerData;

/**
 * Shared scaling for gauge values crossing a {@link net.minecraft.world.inventory.ContainerData} slot.
 *
 * <p>Vanilla syncs each data slot as a <em>short</em>, so any raw value above 32,767 (energy at the
 * configurable capacity of up to 10,000,000 NF, fluid up to 1,000,000&nbsp;mB, recipe ticks up to
 * 72,000) truncates on the wire. Every gauge therefore ships as a permille fraction of its live
 * capacity (0–{@value #SCALE}) — the same technique the sibling Nero mods use — which both survives
 * the 16-bit sync and keeps screens correct when a server raises the configured capacity.</p>
 */
public final class GaugeData {
    /** Denominator every synced gauge fraction is drawn against. */
    public static final int SCALE = 1000;

    private GaugeData() { }

    /** {@code amount/capacity} as 0–{@value #SCALE}, clamped; 0 when the capacity is not positive. */
    public static int permille(long amount, long capacity) {
        if (capacity <= 0) return 0;
        long clamped = Math.max(0, Math.min(amount, capacity));
        return (int) (clamped * SCALE / capacity);
    }

    /** Bits in one half of a split value: 15, so each half survives the signed-short sync. */
    public static final int HALF_BITS = 15;
    private static final int HALF_MASK = (1 << HALF_BITS) - 1;
    /** Largest value a split pair carries (2^30 - 1, about 1.07 billion); larger values clamp. */
    public static final long MAX_SPLIT = (1L << (2 * HALF_BITS)) - 1L;

    /** Data slots needed to carry {@code values} split values. */
    public static int splitSlots(int values) {
        return values * 2;
    }

    /** Low 15 bits of {@code value} clamped to 0..{@link #MAX_SPLIT}. */
    public static int low(long value) {
        return (int) (clampSplit(value) & HALF_MASK);
    }

    /** High 15 bits of {@code value} clamped to 0..{@link #MAX_SPLIT}. */
    public static int high(long value) {
        return (int) ((clampSplit(value) >> HALF_BITS) & HALF_MASK);
    }

    /** Rejoin a value split by {@link #low} and {@link #high}. */
    public static long join(int low, int high) {
        return ((long) (high & HALF_MASK) << HALF_BITS) | (low & HALF_MASK);
    }

    private static long clampSplit(long value) {
        return Math.max(0L, Math.min(MAX_SPLIT, value));
    }

    /**
     * {@code base} followed by each of {@code values} as a low/high pair, so a screen can show exact
     * amounts (gauge tooltips) while the existing fraction slots keep their indices. Read a pair back
     * with {@link #join} at {@code base.getCount() + 2 * i}. Server-side view only: {@code set} is a no-op.
     */
    public static ContainerData withRaw(ContainerData base, LongSupplier... values) {
        int baseCount = base.getCount();
        return new ContainerData() {
            @Override public int get(int index) {
                if (index < baseCount) return base.get(index);
                int extra = index - baseCount;
                int which = extra / 2;
                if (which >= values.length) return 0;
                long value = values[which].getAsLong();
                return (extra & 1) == 0 ? low(value) : high(value);
            }
            @Override public void set(int index, int value) {
                if (index < baseCount) base.set(index, value);
            }
            @Override public int getCount() { return baseCount + splitSlots(values.length); }
        };
    }
}
