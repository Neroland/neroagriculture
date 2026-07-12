package za.co.neroland.neroagriculture.cycle;

/**
 * Bounded multipliers a seasonal/stellar cycle applies to growth, yield and environment. Every value is
 * clamped into a safe range on construction, so no datapack, provider, or combination can push a modifier to
 * an exploitable extreme. The identity modifier (all 1.0) is what a world with no active cycle uses.
 */
public record CycleModifier(float growth, float yield, float environment) {
    public static final float MIN = 0.1F;
    public static final float MAX = 4.0F;
    public static final CycleModifier IDENTITY = new CycleModifier(1.0F, 1.0F, 1.0F);

    public CycleModifier {
        growth = clamp(growth);
        yield = clamp(yield);
        environment = clamp(environment);
    }

    /** Multiply two modifiers (e.g. profile x provider); the result is re-clamped by the constructor. */
    public CycleModifier combine(CycleModifier other) {
        return new CycleModifier(growth * other.growth, yield * other.yield, environment * other.environment);
    }

    private static float clamp(float value) {
        if (!(value > 0.0F)) return MIN;
        return Math.max(MIN, Math.min(MAX, value));
    }
}
