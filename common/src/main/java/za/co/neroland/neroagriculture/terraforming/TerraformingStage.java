package za.co.neroland.neroagriculture.terraforming;

/** Pure staged progress for a terraforming project, derived deterministically from accumulated progress. */
public enum TerraformingStage {
    DORMANT, SEEDED, GROWING, STABILISING, COMPLETE;

    public static TerraformingStage of(boolean seeded, int progress, int total) {
        if (!seeded) return DORMANT;
        if (total <= 0 || progress >= total) return COMPLETE;
        int third = Math.max(1, total / 3);
        if (progress >= 2 * third) return STABILISING;
        if (progress >= third) return GROWING;
        return SEEDED;
    }

    public boolean complete() {
        return this == COMPLETE;
    }

    public static TerraformingStage byOrdinal(int value) {
        return value >= 0 && value < values().length ? values()[value] : DORMANT;
    }
}
