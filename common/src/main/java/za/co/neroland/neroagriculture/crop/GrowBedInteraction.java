package za.co.neroland.neroagriculture.crop;

import net.minecraft.core.Direction;
import org.jetbrains.annotations.Nullable;

import za.co.neroland.neroagriculture.catalog.ResolvedCatalog;
import za.co.neroland.neroagriculture.content.FragmentTier;

public final class GrowBedInteraction {
    private GrowBedInteraction() { }

    public static boolean plantsFromFace(boolean seed, Direction face) {
        return seed && face == Direction.UP;
    }

    @Nullable
    public static GrowthRules.BlockedReason waitingReason(ResolvedCatalog.Status status,
            @Nullable FragmentTier materialTier, FragmentTier bedTier, boolean gateOpen, boolean worldAllowed) {
        if (status == ResolvedCatalog.Status.UNKNOWN || materialTier == null) return GrowthRules.BlockedReason.UNKNOWN_MATERIAL;
        if (status == ResolvedCatalog.Status.DISABLED) return GrowthRules.BlockedReason.DISABLED_MATERIAL;
        if (materialTier.ordinal() > bedTier.ordinal()) return GrowthRules.BlockedReason.WRONG_BED;
        if (!gateOpen) return GrowthRules.BlockedReason.GATE_CLOSED;
        return worldAllowed ? null : GrowthRules.BlockedReason.WRONG_DIMENSION;
    }
}
