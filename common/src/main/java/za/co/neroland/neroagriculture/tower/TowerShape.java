package za.co.neroland.neroagriculture.tower;

/**
 * Pure crop-tower structure maths. A tower is a controller with a run of casing blocks above it; its tier is
 * its height and its capacity is height x slots-per-layer, clamped to the configured maximum. Kept pure so
 * formation and capacity are deterministic and testable without a world.
 */
public final class TowerShape {
    private TowerShape() { }

    /** True when a casing run of {@code height} forms a valid tower within the configured bounds. */
    public static boolean valid(int height, int minHeight, int maxHeight) {
        return height >= Math.max(1, minHeight) && height <= Math.max(1, maxHeight);
    }

    /** Effective height, clamped to the maximum (a taller stack simply caps out). */
    public static int effectiveHeight(int height, int maxHeight) {
        return Math.max(0, Math.min(Math.max(1, maxHeight), height));
    }

    /** Number of virtual crop slots for a formed tower of the given height. */
    public static int slots(int height, int maxHeight, int slotsPerLayer) {
        return effectiveHeight(height, maxHeight) * Math.max(0, slotsPerLayer);
    }

    /** Maximum slot capacity a controller must allocate for. */
    public static int capacity(int maxHeight, int slotsPerLayer) {
        return Math.max(0, maxHeight) * Math.max(0, slotsPerLayer);
    }
}
