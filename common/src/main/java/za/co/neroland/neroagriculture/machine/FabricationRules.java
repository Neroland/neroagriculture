package za.co.neroland.neroagriculture.machine;

import net.minecraft.resources.Identifier;

import za.co.neroland.neroagriculture.content.EssenceCharge;
import za.co.neroland.neroagriculture.content.EssenceFamily;
import za.co.neroland.neroagriculture.content.MaterialVariant;

/** Pure accounting/component checks shared by fabrication runtime and boundary tests. */
public final class FabricationRules {
    private FabricationRules() { }

    public static int energyPerTick(int total, int ticks) {
        if (total < 0 || ticks < 1) throw new IllegalArgumentException("Invalid energy schedule");
        return (total + ticks - 1) / ticks;
    }

    public static boolean materialMatches(MaterialVariant variant, Identifier material, EssenceFamily family) {
        return variant != null && variant.version() == MaterialVariant.CURRENT_VERSION
                && variant.material().equals(material) && variant.family() == family;
    }

    public static boolean chargeMatches(EssenceCharge charge, EssenceFamily family) {
        return charge != null && charge.version() == EssenceCharge.CURRENT_VERSION && charge.family() == family;
    }

    public static boolean transitionAllowed(EssenceFamily source, EssenceFamily destination) {
        return source != null && destination != null && destination.ordinal() == source.ordinal() + 1;
    }

    public static boolean mayComplete(boolean recipeValid, boolean outputFits, boolean hasPower,
            boolean gateStillOpen, boolean componentsStillValid) {
        return recipeValid && outputFits && hasPower && gateStillOpen && componentsStillValid;
    }
}
