package za.co.neroland.neroagriculture.crop;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import za.co.neroland.neroagriculture.catalog.ResolvedCatalog;
import za.co.neroland.neroagriculture.content.EssenceFamily;
import za.co.neroland.neroagriculture.crop.GrowthRules.BlockedReason;
import za.co.neroland.neroagriculture.crop.GrowthRules.Conditions;
import za.co.neroland.neroagriculture.environment.CropClimate;

class GrowthRulesTest {
    @Test
    void oneMaterialPerTierRequiresItsBedAndPoweredTiersRequireResources() {
        for (EssenceFamily tier : EssenceFamily.values()) {
            assertEquals(BlockedReason.NONE, GrowthRules.evaluate(valid(tier)));
            EssenceFamily lower = tier == EssenceFamily.TERRAN ? null : EssenceFamily.values()[tier.ordinal() - 1];
            if (lower != null) {
                assertEquals(BlockedReason.WRONG_BED, GrowthRules.evaluate(new Conditions(
                        ResolvedCatalog.Status.ACTIVE, tier, lower, true, true, true, CropClimate.Result.OK, true, true)));
                assertEquals(BlockedReason.NO_POWER, GrowthRules.evaluate(new Conditions(
                        ResolvedCatalog.Status.ACTIVE, tier, tier, true, true, true, CropClimate.Result.OK, false, true)));
                assertEquals(BlockedReason.NO_NUTRIENT, GrowthRules.evaluate(new Conditions(
                        ResolvedCatalog.Status.ACTIVE, tier, tier, true, true, true, CropClimate.Result.OK, true, false)));
            }
        }
    }

    @Test
    void catalogGateLightAndDimensionFailClosedInStableOrder() {
        Conditions valid = valid(EssenceFamily.ORBITAL);
        assertEquals(BlockedReason.UNKNOWN_MATERIAL, GrowthRules.evaluate(new Conditions(
                ResolvedCatalog.Status.UNKNOWN, valid.materialTier(), valid.bedTier(), true, true, true,
                CropClimate.Result.OK, true, true)));
        assertEquals(BlockedReason.DISABLED_MATERIAL, GrowthRules.evaluate(new Conditions(
                ResolvedCatalog.Status.DISABLED, valid.materialTier(), valid.bedTier(), true, true, true,
                CropClimate.Result.OK, true, true)));
        assertEquals(BlockedReason.GATE_CLOSED, GrowthRules.evaluate(new Conditions(
                valid.catalogStatus(), valid.materialTier(), valid.bedTier(), false, true, true,
                CropClimate.Result.OK, true, true)));
        assertEquals(BlockedReason.LOW_LIGHT, GrowthRules.evaluate(new Conditions(
                valid.catalogStatus(), valid.materialTier(), valid.bedTier(), true, false, true,
                CropClimate.Result.OK, true, true)));
        assertEquals(BlockedReason.WRONG_DIMENSION, GrowthRules.evaluate(new Conditions(
                valid.catalogStatus(), valid.materialTier(), valid.bedTier(), true, true, false,
                CropClimate.Result.OK, true, true)));
    }

    @Test
    void hostileEnvironmentAndGreenhouseRequirementFailClosedAfterDimension() {
        Conditions base = valid(EssenceFamily.INDUSTRIAL);
        assertEquals(BlockedReason.HOSTILE_ENVIRONMENT, GrowthRules.evaluate(new Conditions(
                base.catalogStatus(), base.materialTier(), base.bedTier(), true, true, true,
                CropClimate.Result.HOSTILE_ENVIRONMENT, true, true)));
        assertEquals(BlockedReason.NEEDS_GREENHOUSE, GrowthRules.evaluate(new Conditions(
                base.catalogStatus(), base.materialTier(), base.bedTier(), true, true, true,
                CropClimate.Result.NEEDS_GREENHOUSE, true, true)));
    }

    private static Conditions valid(EssenceFamily tier) {
        return new Conditions(ResolvedCatalog.Status.ACTIVE, tier, tier, true, true, true,
                CropClimate.Result.OK, true, true);
    }
}
