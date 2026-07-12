package za.co.neroland.neroagriculture.environment;

/**
 * Pure, bounded oxygen-contribution maths for greenhouse life support. A crop's oxygen scales with maturity
 * and its oxygen-output genetics; the greenhouse total is clamped by a hard per-volume cap so a few plants
 * can never yield unlimited free upkeep.
 */
public final class OxygenContribution {
    private OxygenContribution() { }

    /** Oxygen from one crop: base production scaled by maturity, plus the (already-capped) genetics bonus. */
    public static int perCrop(int oxygenProduction, int age, int maxAge, int oxygenGenetics) {
        int base = maxAge <= 0 ? Math.max(0, oxygenProduction)
                : Math.max(0, oxygenProduction) * Math.max(0, Math.min(age, maxAge)) / maxAge;
        return base + Math.max(0, oxygenGenetics);
    }

    /** Clamp the summed contribution to a hard cap that grows with interior volume. */
    public static int capped(int total, int volume, int perUnitVolumeCap, int unitVolume) {
        int units = Math.max(1, (Math.max(0, volume) + Math.max(1, unitVolume) - 1) / Math.max(1, unitVolume));
        int cap = units * Math.max(0, perUnitVolumeCap);
        return Math.max(0, Math.min(Math.max(0, total), cap));
    }
}
