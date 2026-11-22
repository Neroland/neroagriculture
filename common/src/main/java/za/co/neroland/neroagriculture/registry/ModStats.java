package za.co.neroland.neroagriculture.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;

import za.co.neroland.neroagriculture.NeroAgricultureCommon;
import za.co.neroland.nerolandcore.registry.RegistrationProvider;
import za.co.neroland.nerolandcore.registry.RegistrationProvider.RegistryEntry;

/**
 * NeroAgriculture's custom statistics (vanilla {@code minecraft:custom} stat type). They live in the
 * player's vanilla statistics file like any other stat and add no store of the mod's own.
 */
public final class ModStats {
    public static final RegistrationProvider<Identifier> STATS =
            RegistrationProvider.get(Registries.CUSTOM_STAT, NeroAgricultureCommon.MOD_ID);

    /** Mature NeroAgriculture crops the player harvested, by hand or by breaking them. */
    public static final RegistryEntry<Identifier> CROPS_HARVESTED = STATS.register("crops_harvested",
            key -> Identifier.fromNamespaceAndPath(NeroAgricultureCommon.MOD_ID, "crops_harvested"));

    private ModStats() { }

    public static void init() { }
}
