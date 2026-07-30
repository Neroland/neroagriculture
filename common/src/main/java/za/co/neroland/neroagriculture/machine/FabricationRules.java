package za.co.neroland.neroagriculture.machine;

import net.minecraft.resources.Identifier;

import za.co.neroland.neroagriculture.content.FragmentCharge;
import za.co.neroland.neroagriculture.content.FragmentTier;
import za.co.neroland.neroagriculture.content.MaterialVariant;

/** Pure accounting/component checks shared by fabrication runtime and boundary tests. */
public final class FabricationRules {
    private FabricationRules() { }

    public static int energyPerTick(int total, int ticks) {
        if (total < 0 || ticks < 1) throw new IllegalArgumentException("Invalid energy schedule");
        return (total + ticks - 1) / ticks;
    }

    public static boolean materialMatches(MaterialVariant variant, Identifier material, FragmentTier family) {
        return variant != null && variant.version() == MaterialVariant.CURRENT_VERSION
                && variant.material().equals(material) && variant.family() == family;
    }

    public static boolean chargeMatches(FragmentCharge charge, FragmentTier family) {
        return charge != null && charge.version() == FragmentCharge.CURRENT_VERSION && charge.family() == family;
    }

    public static boolean transitionAllowed(FragmentTier source, FragmentTier destination) {
        return source != null && destination != null && destination.ordinal() == source.ordinal() + 1;
    }

    /**
     * Number of matching Tier Fragments consumed, alongside one Prospora Seed base and the real
     * resource, to synthesize one Resource Seed. Scales with tier so higher-tier seeds cost more of the
     * (harder-won) higher-tier fragments.
     */
    public static int fragmentsPerSeed(FragmentTier tier) {
        return 2 + tier.ordinal();
    }

    public static boolean mayComplete(boolean recipeValid, boolean outputFits, boolean hasPower,
            boolean gateStillOpen, boolean componentsStillValid) {
        return recipeValid && outputFits && hasPower && gateStillOpen && componentsStillValid;
    }
}
