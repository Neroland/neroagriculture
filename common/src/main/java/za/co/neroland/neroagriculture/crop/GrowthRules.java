package za.co.neroland.neroagriculture.crop;

import za.co.neroland.neroagriculture.catalog.ResolvedCatalog;
import za.co.neroland.neroagriculture.content.EssenceFamily;
import za.co.neroland.neroagriculture.environment.CropClimate;

/** Pure fail-closed condition ordering shared by random growth and deterministic tests. */
public final class GrowthRules {
    public enum BlockedReason {
        NONE, UNKNOWN_MATERIAL, DISABLED_MATERIAL, GATE_CLOSED, WRONG_BED, LOW_LIGHT,
        WRONG_DIMENSION, HOSTILE_ENVIRONMENT, NEEDS_GREENHOUSE, NO_POWER, NO_NUTRIENT
    }

    public record Conditions(ResolvedCatalog.Status catalogStatus, EssenceFamily materialTier,
            EssenceFamily bedTier, boolean gateOpen, boolean lightEnough, boolean dimensionAllowed,
            CropClimate.Result climate, boolean hasPower, boolean hasNutrient) { }

    private GrowthRules() { }

    public static BlockedReason evaluate(Conditions conditions) {
        if (conditions.catalogStatus == ResolvedCatalog.Status.UNKNOWN) return BlockedReason.UNKNOWN_MATERIAL;
        if (conditions.catalogStatus == ResolvedCatalog.Status.DISABLED) return BlockedReason.DISABLED_MATERIAL;
        if (!conditions.gateOpen) return BlockedReason.GATE_CLOSED;
        if (conditions.bedTier == null || conditions.bedTier.ordinal() < conditions.materialTier.ordinal()) {
            return BlockedReason.WRONG_BED;
        }
        if (!conditions.lightEnough) return BlockedReason.LOW_LIGHT;
        if (!conditions.dimensionAllowed) return BlockedReason.WRONG_DIMENSION;
        if (conditions.climate == CropClimate.Result.HOSTILE_ENVIRONMENT) return BlockedReason.HOSTILE_ENVIRONMENT;
        if (conditions.climate == CropClimate.Result.NEEDS_GREENHOUSE) return BlockedReason.NEEDS_GREENHOUSE;
        if (conditions.materialTier != EssenceFamily.TERRAN && !conditions.hasPower) return BlockedReason.NO_POWER;
        if (conditions.materialTier != EssenceFamily.TERRAN && !conditions.hasNutrient) return BlockedReason.NO_NUTRIENT;
        return BlockedReason.NONE;
    }
}
