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
}
