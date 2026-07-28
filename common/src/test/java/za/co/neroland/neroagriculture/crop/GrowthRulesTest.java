package za.co.neroland.neroagriculture.crop;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import za.co.neroland.neroagriculture.catalog.ResolvedCatalog;
import za.co.neroland.neroagriculture.content.FragmentTier;
import za.co.neroland.neroagriculture.crop.GrowthRules.BlockedReason;
import za.co.neroland.neroagriculture.crop.GrowthRules.Conditions;
import za.co.neroland.neroagriculture.environment.CropClimate;

class GrowthRulesTest {
    @Test
    void oneMaterialPerTierRequiresItsBedAndPoweredTiersRequireResources() {
        for (FragmentTier tier : FragmentTier.values()) {
            assertEquals(BlockedReason.NONE, GrowthRules.evaluate(valid(tier)));
            FragmentTier lower = tier == FragmentTier.TERRITE ? null : FragmentTier.values()[tier.ordinal() - 1];
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
        Conditions valid = valid(FragmentTier.ORBITE);
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
        Conditions base = valid(FragmentTier.FORGITE);
        assertEquals(BlockedReason.HOSTILE_ENVIRONMENT, GrowthRules.evaluate(new Conditions(
                base.catalogStatus(), base.materialTier(), base.bedTier(), true, true, true,
                CropClimate.Result.HOSTILE_ENVIRONMENT, true, true)));
        assertEquals(BlockedReason.NEEDS_GREENHOUSE, GrowthRules.evaluate(new Conditions(
                base.catalogStatus(), base.materialTier(), base.bedTier(), true, true, true,
                CropClimate.Result.NEEDS_GREENHOUSE, true, true)));
    }

    @Test
    void towerHasNoBedTierButChargesEveryTierForPowerAndNutrient() {
        assertEquals(BlockedReason.NONE, GrowthRules.evaluateTower(ResolvedCatalog.Status.ACTIVE, true, true, true));
        assertEquals(BlockedReason.UNKNOWN_MATERIAL,
                GrowthRules.evaluateTower(ResolvedCatalog.Status.UNKNOWN, true, true, true));
        assertEquals(BlockedReason.DISABLED_MATERIAL,
                GrowthRules.evaluateTower(ResolvedCatalog.Status.DISABLED, true, true, true));
        assertEquals(BlockedReason.GATE_CLOSED,
                GrowthRules.evaluateTower(ResolvedCatalog.Status.ACTIVE, false, true, true));
        assertEquals(BlockedReason.NO_POWER,
                GrowthRules.evaluateTower(ResolvedCatalog.Status.ACTIVE, true, false, true));
        assertEquals(BlockedReason.NO_NUTRIENT,
                GrowthRules.evaluateTower(ResolvedCatalog.Status.ACTIVE, true, true, false));
        // A TERRITE crop is exempt from the bed's resource draw but never from the tower's.
        assertEquals(BlockedReason.NONE, GrowthRules.evaluate(new Conditions(ResolvedCatalog.Status.ACTIVE,
                FragmentTier.TERRITE, FragmentTier.TERRITE, true, true, true, CropClimate.Result.OK, false, false)));
        assertEquals(BlockedReason.NO_POWER,
                GrowthRules.evaluateTower(ResolvedCatalog.Status.ACTIVE, true, false, false));
    }

    @Test
    void aMatureTowerSlotIsBlockedByAFullOutputClusterRatherThanByPowerOrNutrient() {
        assertEquals(BlockedReason.NONE,
                GrowthRules.evaluateTowerHarvest(ResolvedCatalog.Status.ACTIVE, true, true));
        assertEquals(BlockedReason.OUTPUT_FULL,
                GrowthRules.evaluateTowerHarvest(ResolvedCatalog.Status.ACTIVE, true, false));
        // Catalogue and gate still come first, in the same fail-closed order growth uses.
        assertEquals(BlockedReason.UNKNOWN_MATERIAL,
                GrowthRules.evaluateTowerHarvest(ResolvedCatalog.Status.UNKNOWN, true, false));
        assertEquals(BlockedReason.DISABLED_MATERIAL,
                GrowthRules.evaluateTowerHarvest(ResolvedCatalog.Status.DISABLED, true, false));
        assertEquals(BlockedReason.GATE_CLOSED,
                GrowthRules.evaluateTowerHarvest(ResolvedCatalog.Status.ACTIVE, false, false));
    }

    @Test
    void aggregatingSlotsNeverHidesABlockedOneBehindAHealthyOne() {
        assertEquals(BlockedReason.NONE, GrowthRules.worst(BlockedReason.NONE, BlockedReason.NONE));
        assertEquals(BlockedReason.OUTPUT_FULL, GrowthRules.worst(BlockedReason.NONE, BlockedReason.OUTPUT_FULL));
        assertEquals(BlockedReason.OUTPUT_FULL, GrowthRules.worst(BlockedReason.OUTPUT_FULL, BlockedReason.NONE));
        // Declaration order is severity order: the more fundamental blocker wins, either way round.
        assertEquals(BlockedReason.GATE_CLOSED,
                GrowthRules.worst(BlockedReason.NO_NUTRIENT, BlockedReason.GATE_CLOSED));
        assertEquals(BlockedReason.GATE_CLOSED,
                GrowthRules.worst(BlockedReason.GATE_CLOSED, BlockedReason.NO_NUTRIENT));
        // A missing shared resource stalls every slot, so it outranks a jam that only stalls ripe ones.
        assertEquals(BlockedReason.NO_POWER, GrowthRules.worst(BlockedReason.OUTPUT_FULL, BlockedReason.NO_POWER));
    }

    @Test
    void theWireOrdinalsOfTheOriginalBlockersNeverMove() {
        // BlockedReason travels as a ContainerData int, so new reasons may only ever be appended.
        assertEquals(0, BlockedReason.NONE.ordinal());
        assertEquals(10, BlockedReason.NO_NUTRIENT.ordinal());
        assertEquals(11, BlockedReason.NOT_FORMED.ordinal());
        assertEquals(12, BlockedReason.OUTPUT_FULL.ordinal());
    }

    private static Conditions valid(FragmentTier tier) {
        return new Conditions(ResolvedCatalog.Status.ACTIVE, tier, tier, true, true, true,
                CropClimate.Result.OK, true, true);
    }
}
