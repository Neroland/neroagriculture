package za.co.neroland.neroagriculture.fertiliser;

/** Two distinct fertiliser formulations. Their effects are separate and each is capped independently. */
public enum FertiliserType {
    SPEED, YIELD;

    public static FertiliserType byOrdinal(int value) {
        return value >= 0 && value < values().length ? values()[value] : SPEED;
    }
}
