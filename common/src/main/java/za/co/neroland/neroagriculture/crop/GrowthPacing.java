package za.co.neroland.neroagriculture.crop;

/** Random-tick pacing for resource crops. */
public final class GrowthPacing {
    private GrowthPacing() { }

    public static double chance(double multiplier, double cycle) {
        return Math.max(0.0, Math.min(1.0, multiplier * cycle));
    }

    public static int ageStep(int improvedStep) {
        return Math.max(1, improvedStep) + 1;
    }
}
