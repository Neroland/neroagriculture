package za.co.neroland.neroagriculture.fluid;

import net.minecraft.world.item.BucketItem;
import net.minecraft.world.level.material.Fluid;

import za.co.neroland.neroagriculture.registry.ModBlocks;
import za.co.neroland.neroagriculture.registry.ModItems;
import za.co.neroland.nerolandcore.registry.RegistrationProvider.RegistryEntry;

/**
 * The fluids NeroAgriculture registers.
 *
 * <p>Each kind owns its own source/flowing pair, bucket and liquid block, so the per-loader
 * {@link za.co.neroland.neroagriculture.platform.FluidFactory} implementations can build one set of
 * fluid properties <em>per fluid</em>. Sharing a single set is what made biofuel resolve to
 * nutrient's bucket and liquid block, so a placed biofuel source turned into nutrient.</p>
 *
 * <p>Every accessor returns the registry entry rather than the value: this enum is read while
 * {@link ModFluids} is still running its own static initialiser, so nothing may be resolved eagerly.
 * The returned entry is a supplier and is safe to hand straight to the loader fluid properties.</p>
 */
public enum FluidKind {
    /** Nutrient solution — denser and thicker than water; feeds grow beds and the crop tower. */
    NUTRIENT("nutrient", 1050, 1100),
    /** Biofuel — lighter and runnier than nutrient, closer to an oil. */
    BIOFUEL("biofuel", 900, 1000);

    private final String id;
    private final int density;
    private final int viscosity;

    FluidKind(String id, int density, int viscosity) {
        this.id = id;
        this.density = density;
        this.viscosity = viscosity;
    }

    /**
     * Registry path shared by the source fluid, the liquid block and the {@code block/<id>_still} and
     * {@code block/<id>_flow} textures. The flowing fluid is registered as {@code flowing_<id>}.
     */
    public String id() {
        return this.id;
    }

    /** Fluid density in the loader fluid-type sense; water is 1000. */
    public int density() {
        return this.density;
    }

    /** Fluid viscosity in the loader fluid-type sense; water is 1000. */
    public int viscosity() {
        return this.viscosity;
    }

    public RegistryEntry<Fluid> source() {
        return this == NUTRIENT ? ModFluids.NUTRIENT : ModFluids.BIOFUEL;
    }

    public RegistryEntry<Fluid> flowing() {
        return this == NUTRIENT ? ModFluids.FLOWING_NUTRIENT : ModFluids.FLOWING_BIOFUEL;
    }

    public RegistryEntry<BucketItem> bucket() {
        return this == NUTRIENT ? ModItems.NUTRIENT_BUCKET : ModItems.BIOFUEL_BUCKET;
    }

    public RegistryEntry<NutrientLiquidBlock> block() {
        return this == NUTRIENT ? ModBlocks.NUTRIENT : ModBlocks.BIOFUEL;
    }
}
