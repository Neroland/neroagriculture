package za.co.neroland.neroagriculture.menu;

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
}
