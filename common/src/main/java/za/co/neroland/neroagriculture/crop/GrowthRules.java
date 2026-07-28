package za.co.neroland.neroagriculture.crop;

import za.co.neroland.neroagriculture.catalog.ResolvedCatalog;
import za.co.neroland.neroagriculture.content.FragmentTier;
import za.co.neroland.neroagriculture.environment.CropClimate;

/** Pure fail-closed condition ordering shared by random growth and deterministic tests. */
public final class GrowthRules {
    /**
     * Ordered most fundamental first, which is both the order {@link #evaluate} reports them in and the
     * severity order {@link #worst} aggregates by. The ordinal is the wire value carried by the menus'
     * {@code ContainerData}, so new reasons are only ever <em>appended</em>.
     *
     * <p>{@code NOT_FORMED} and {@code OUTPUT_FULL} are crop-tower blockers: a tower whose casing column
     * is short never runs a cycle at all, and a mature slot goes to harvest rather than growth, where the
     * only remaining blocker is an output cluster with no room left.</p>
     */
    public enum BlockedReason {
        NONE, UNKNOWN_MATERIAL, DISABLED_MATERIAL, GATE_CLOSED, WRONG_BED, LOW_LIGHT,
        WRONG_DIMENSION, HOSTILE_ENVIRONMENT, NEEDS_GREENHOUSE, NO_POWER, NO_NUTRIENT,
        NOT_FORMED, OUTPUT_FULL
    }

    public record Conditions(ResolvedCatalog.Status catalogStatus, FragmentTier materialTier,
            FragmentTier bedTier, boolean gateOpen, boolean lightEnough, boolean dimensionAllowed,
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
        if (conditions.materialTier != FragmentTier.TERRITE && !conditions.hasPower) return BlockedReason.NO_POWER;
        if (conditions.materialTier != FragmentTier.TERRITE && !conditions.hasNutrient) return BlockedReason.NO_NUTRIENT;
        return BlockedReason.NONE;
    }

    /**
     * Blocker ordering for the crop tower, which has no bed and no sky: only the catalogue status, the
     * progression gate and the tower's own NF/nutrient draw can stop a slot. Climate never <em>blocks</em>
     * a tower — a layer whose environment would fail the grow-bed climate check instead pays the
     * configured hostile-environment NF surcharge in the controller's energy accounting. Unlike a grow
     * bed the tower charges every tier, so TERRITE is not exempt from the resource checks here.
     */
    public static BlockedReason evaluateTower(ResolvedCatalog.Status catalogStatus, boolean gateOpen,
            boolean hasPower, boolean hasNutrient) {
        if (catalogStatus == ResolvedCatalog.Status.UNKNOWN) return BlockedReason.UNKNOWN_MATERIAL;
        if (catalogStatus == ResolvedCatalog.Status.DISABLED) return BlockedReason.DISABLED_MATERIAL;
        if (!gateOpen) return BlockedReason.GATE_CLOSED;
        if (!hasPower) return BlockedReason.NO_POWER;
        if (!hasNutrient) return BlockedReason.NO_NUTRIENT;
        return BlockedReason.NONE;
    }

    /**
     * Blocker ordering for a <em>mature</em> crop-tower slot, which the cycle sends to harvest rather than
     * to growth. Harvesting re-checks the catalogue status and the progression gate exactly as growth does,
     * but draws no NF and no nutrient — so once those pass, the only thing that can stall the slot is an
     * output cluster with no room for the fragment.
     */
    public static BlockedReason evaluateTowerHarvest(ResolvedCatalog.Status catalogStatus, boolean gateOpen,
            boolean outputAccepts) {
        if (catalogStatus == ResolvedCatalog.Status.UNKNOWN) return BlockedReason.UNKNOWN_MATERIAL;
        if (catalogStatus == ResolvedCatalog.Status.DISABLED) return BlockedReason.DISABLED_MATERIAL;
        if (!gateOpen) return BlockedReason.GATE_CLOSED;
        if (!outputAccepts) return BlockedReason.OUTPUT_FULL;
        return BlockedReason.NONE;
    }

    /**
     * The more fundamental of two blockers, for machines that work several crops against one status line.
     * {@link BlockedReason#NONE} only survives when nothing is blocked, so a single stalled slot is never
     * hidden behind a healthy one; otherwise declaration order decides, which is the same fail-closed
     * order {@link #evaluate} reports in. The appended tower reasons therefore rank last on purpose: a
     * missing resource stalls every slot, while a jammed output only stalls the ones that ripened.
     */
    public static BlockedReason worst(BlockedReason first, BlockedReason second) {
        if (first == BlockedReason.NONE) return second;
        if (second == BlockedReason.NONE) return first;
        return first.ordinal() <= second.ordinal() ? first : second;
    }
}
