package za.co.neroland.neroagriculture.fertiliser;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/** Reads a powered bed's active fertiliser doses to boost growth speed or harvest yield, bounded by caps. */
public final class Fertilisers {
    private Fertilisers() { }

    /** Age steps per growth tick: 2 when a bed carries an active SPEED dose, otherwise 1. */
    public static int speedStep(Level level, BlockPos bedPos) {
        return activeAmount(level, bedPos, FertiliserType.SPEED) > 0 ? 2 : 1;
    }

    /** Flat yield bonus from a bed's active YIELD dose (already capped when applied). */
    public static int yieldBonus(Level level, BlockPos bedPos) {
        return activeAmount(level, bedPos, FertiliserType.YIELD);
    }

    private static int activeAmount(Level level, BlockPos bedPos, FertiliserType type) {
        if (level.getBlockEntity(bedPos) instanceof FertilisableBed bed) {
            FertiliserDose dose = bed.activeDose(type, level.getGameTime());
            if (dose != null) return dose.amount();
        }
        return 0;
    }
}
