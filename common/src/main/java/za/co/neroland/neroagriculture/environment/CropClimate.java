package za.co.neroland.neroagriculture.environment;

/**
 * Pure crop-environment evaluation shared by growth and tests. A crop grows when it sits in a sealed,
 * powered greenhouse, or — for tiers below the controlled-environment threshold — in a habitable open-air
 * world. Higher tiers always require an engineered (sealed) environment. Hardiness may later relax an
 * individual condition, but only within its configured trait cap (Stage 10).
 */
public final class CropClimate {
    public enum Result { OK, HOSTILE_ENVIRONMENT, NEEDS_GREENHOUSE }

    private CropClimate() { }

    public static Result evaluate(EnvironmentProfile world, boolean sealed, int tierOrdinal,
            int controlledThresholdOrdinal) {
        if (sealed) return Result.OK;
        if (tierOrdinal >= controlledThresholdOrdinal) return Result.NEEDS_GREENHOUSE;
        return world.habitable() ? Result.OK : Result.HOSTILE_ENVIRONMENT;
    }

    /** Parse the configured controlled-environment threshold tier to an ordinal; defaults to Orbital. */
    public static int thresholdOrdinal(String tierName) {
        try {
            return za.co.neroland.neroagriculture.content.EssenceFamily
                    .valueOf(tierName.toUpperCase(java.util.Locale.ROOT)).ordinal();
        } catch (RuntimeException e) {
            return za.co.neroland.neroagriculture.content.EssenceFamily.ORBITAL.ordinal();
        }
    }
}
