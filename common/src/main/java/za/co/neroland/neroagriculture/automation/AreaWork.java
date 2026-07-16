package za.co.neroland.neroagriculture.automation;

import net.minecraft.core.BlockPos;

/**
 * Pure, deterministic square work-area maths shared by the Planter/Harvester and tests. The area is a
 * (2r+1)x(2r+1) column grid centred on the machine; work is processed a bounded number of columns per pass
 * from a persisted cursor so there is never a per-tick full-area scan.
 */
public final class AreaWork {
    public static final int MIN_RADIUS = 3; // 7x7 base (3 blocks on either side)
    public static final int MAX_RADIUS = 6; // 13x13 with range upgrades

    private AreaWork() { }

    /** Radius (in columns) from the installed RANGE upgrade count, clamped to the 7x7..13x13 range. */
    public static int radius(int rangeUpgrades) {
        return Math.max(MIN_RADIUS, Math.min(MAX_RADIUS, MIN_RADIUS + Math.max(0, rangeUpgrades)));
    }

    /** Number of columns in the area for a radius. */
    public static int columns(int radius) {
        int side = 2 * radius + 1;
        return side * side;
    }

    /** Deterministic column position for a linear index in [0, columns(radius)); wraps out-of-range indices. */
    public static BlockPos columnAt(BlockPos center, int radius, int index) {
        int side = 2 * radius + 1;
        int local = Math.floorMod(index, side * side);
        int dx = local % side - radius;
        int dz = local / side - radius;
        return center.offset(dx, 0, dz);
    }

    /** Next cursor value after processing one pass of {@code perPass} columns. */
    public static int advanceCursor(int cursor, int perPass, int radius) {
        return Math.floorMod(cursor + Math.max(0, perPass), columns(radius));
    }

    /** A stable per-position phase offset so neighbouring machines do not all work on the same tick. */
    public static int phaseOffset(BlockPos pos, int interval) {
        if (interval <= 1) return 0;
        return Math.floorMod(pos.getX() * 31 + pos.getZ() * 17 + pos.getY(), interval);
    }
}
