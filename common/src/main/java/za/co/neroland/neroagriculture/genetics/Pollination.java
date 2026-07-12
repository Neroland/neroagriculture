package za.co.neroland.neroagriculture.genetics;

/** Pure, deterministic pollination rolls and child-genetics maths, shared by field crops, the beacon, and tests. */
public final class Pollination {
    private Pollination() { }

    /** Deterministic yes/no from a seed and a 0-100 percent chance. */
    public static boolean roll(long seed, int chancePercent) {
        if (chancePercent <= 0) return false;
        if (chancePercent >= 100) return true;
        return Math.floorMod(mix(seed), 100) < chancePercent;
    }

    /** Child genetics: the spliced parents, optionally mutated one step by a deterministic seed. */
    public static Genetics childGenetics(Genetics a, Genetics b, boolean mutate, long seed) {
        Genetics spliced = Genetics.splice(a, b);
        return mutate ? spliced.mutated(mix(seed) >> 3) : spliced;
    }

    private static long mix(long seed) {
        long z = seed + 0x9E3779B97F4A7C15L;
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }
}
