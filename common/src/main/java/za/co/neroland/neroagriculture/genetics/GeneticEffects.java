package za.co.neroland.neroagriculture.genetics;

/** Pure, bounded conversions from capped genetics to their in-world effects. */
public final class GeneticEffects {
    private GeneticEffects() { }

    /** Growth age-steps per tick: the fertiliser step plus up to +2 from the speed trait. */
    public static int growthStep(int fertiliserStep, Genetics genetics) {
        return Math.max(1, fertiliserStep + genetics.speed() / 2);
    }

    /** Flat harvest bonus from the yield trait (already capped at 5). */
    public static int yieldBonus(Genetics genetics) {
        return genetics.yield();
    }

    /** Effect amplifier for food, raised by the food-potency trait but never above the species cap. */
    public static int foodAmplifier(int baseAmplifier, int potencyCap, Genetics genetics) {
        return Math.min(potencyCap, baseAmplifier + genetics.foodPotency());
    }
}
