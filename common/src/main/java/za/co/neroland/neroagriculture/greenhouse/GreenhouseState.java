package za.co.neroland.neroagriculture.greenhouse;

/** Synced, ordinal-stable greenhouse status used by the controller and status readout. */
public enum GreenhouseState {
    UNFORMED, FORMED, BREACHED, UNPOWERED;

    public static GreenhouseState byOrdinal(int value) {
        return value >= 0 && value < values().length ? values()[value] : UNFORMED;
    }

    /** A greenhouse only maintains its controlled interior when formed and powered. */
    public boolean maintainsInterior() {
        return this == FORMED;
    }
}
