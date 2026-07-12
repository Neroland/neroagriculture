package za.co.neroland.neroagriculture.fertiliser;

import org.jetbrains.annotations.Nullable;

/** Implemented by powered grow-bed block entities that can hold a timed fertiliser dose. */
public interface FertilisableBed {
    /** Apply/refresh a dose of the given type, clamped to the cap; returns true if anything changed. */
    boolean applyFertiliser(FertiliserType type, int amount, long now, int durationTicks, int maxDose);

    /** The active dose of the given type, or null if none/expired. */
    @Nullable
    FertiliserDose activeDose(FertiliserType type, long now);
}
