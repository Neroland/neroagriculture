package za.co.neroland.neroagriculture.biofuel;

/**
 * Pure biofuel energy accounting. Default biofuel is a renewable baseload whose NF value per millibucket
 * must stay strictly below an equivalently-gated primary generator's ceiling, so a farm can never out-earn
 * dedicated power. Shared by the converter, the provider seam and tests.
 */
public final class Biofuel {
    private Biofuel() { }

    /** NF energy in a quantity of biofuel. */
    public static long energyNf(long amountMb, int energyPerMb) {
        return Math.max(0L, amountMb) * Math.max(0, energyPerMb);
    }

    /** True when biofuel stays below the configured primary-generation ceiling (per mB). */
    public static boolean belowPrimaryCeiling(int energyPerMb, int ceilingPerMb) {
        return energyPerMb < ceilingPerMb;
    }
}
