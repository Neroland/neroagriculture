package za.co.neroland.neroagriculture.environment;

/**
 * The environment at a growing position: temperature band plus whether the air is oxygenated and
 * pressurised. A profile is "habitable" for open-air growth only when all three are crop-friendly.
 */
public record EnvironmentProfile(Temperature temperature, boolean oxygenated, boolean pressurised) {

    /** The engineered interior a formed, powered greenhouse maintains. */
    public static final EnvironmentProfile CONTROLLED = new EnvironmentProfile(Temperature.TEMPERATE, true, true);
    /** The safe open-air default for Earth-like and unclassified dimensions. */
    public static final EnvironmentProfile HABITABLE = CONTROLLED;

    /** True when unprotected crops can grow here without a sealed greenhouse. */
    public boolean habitable() {
        return temperature == Temperature.TEMPERATE && oxygenated && pressurised;
    }
}
