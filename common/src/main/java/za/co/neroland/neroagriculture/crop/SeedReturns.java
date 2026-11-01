package za.co.neroland.neroagriculture.crop;

/** One reproduction opportunity per seed, including after harvesting, breaking and replanting. */
public final class SeedReturns {
    private SeedReturns() { }

    public static boolean awards(int previousHarvests, int chancePercent, int roll) {
        return previousHarvests == 0 && chancePercent > 0 && roll >= 0 && roll < chancePercent;
    }
}
