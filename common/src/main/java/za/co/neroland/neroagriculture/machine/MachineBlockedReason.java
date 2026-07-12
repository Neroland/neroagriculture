package za.co.neroland.neroagriculture.machine;

/** Synced, ordinal-stable machine state used by menus and diagnostics. */
public enum MachineBlockedReason {
    IDLE, NO_RECIPE, INVALID_COMPONENT, CATALOG_DISABLED, GATE_CLOSED, MILESTONE_REQUIRED,
    RESEARCH_REQUIRED, NO_POWER, OUTPUT_FULL, RUNNING, COMPLETE;

    public static MachineBlockedReason byOrdinal(int value) {
        return value >= 0 && value < values().length ? values()[value] : IDLE;
    }
}
