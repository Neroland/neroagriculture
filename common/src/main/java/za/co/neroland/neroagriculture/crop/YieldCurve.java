package za.co.neroland.neroagriculture.crop;

import za.co.neroland.neroagriculture.catalog.MaterialDefinition;

/** Deterministic integer ramp; never exceeds the definition's tier cap before global scaling. */
public final class YieldCurve {
    private YieldCurve() { }

    public static int base(MaterialDefinition.Yield yield, int harvestCount) {
        if (yield.maximum() == yield.minimum() || yield.rampHarvests() == 0) return yield.maximum();
        int progress = Math.max(0, Math.min(harvestCount, yield.rampHarvests()));
        return yield.minimum() + (yield.maximum() - yield.minimum()) * progress / yield.rampHarvests();
    }

    public static int scaled(MaterialDefinition.Yield yield, int harvestCount, double multiplier) {
        return Math.max(0, Math.min(4096, (int) Math.floor(base(yield, harvestCount) * Math.max(0.0, multiplier))));
    }

    /** Global-scaled yield clamped by the absolute per-tier ceiling; overrides can never exceed the cap. */
    public static int scaledCapped(MaterialDefinition.Yield yield, int harvestCount, double multiplier, int tierCap) {
        return Math.min(Math.max(0, tierCap), scaled(yield, harvestCount, multiplier));
    }

    public static int harvest(MaterialDefinition.Yield yield, int harvestCount, double multiplier,
            double cycleMultiplier, int bonus, int tierCap) {
        int cap = Math.max(0, tierCap);
        int scaled = scaled(yield, harvestCount, multiplier);
        double cycle = Math.max(0.0, cycleMultiplier);
        double seasonal = Math.floor(scaled * cycle);
        if (scaled > 0 && cycle > 0.0) seasonal = Math.max(1.0, seasonal);
        double amount = seasonal + Math.max(0, bonus);
        return (int) Math.max(0, Math.min(cap, amount));
    }

    /** Capped yield after one more harvest, for current/next/max diagnostics. Saturates at the ramp end. */
    public static int nextCapped(MaterialDefinition.Yield yield, int harvestCount, double multiplier, int tierCap) {
        return scaledCapped(yield, harvestCount + 1, multiplier, tierCap);
    }

    /** Capped yield a fully invested crop reaches, for current/next/max diagnostics. */
    public static int maxCapped(MaterialDefinition.Yield yield, double multiplier, int tierCap) {
        return scaledCapped(yield, yield.rampHarvests(), multiplier, tierCap);
    }
}
