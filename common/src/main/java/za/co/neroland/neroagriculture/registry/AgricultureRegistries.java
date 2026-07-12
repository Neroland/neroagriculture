package za.co.neroland.neroagriculture.registry;

/** Deterministic class-load order for all vanilla registry holders. */
public final class AgricultureRegistries {
    private AgricultureRegistries() { }

    public static void init() {
        za.co.neroland.neroagriculture.fluid.ModFluids.init();
        ModDataComponents.init();
        ModMobEffects.init();
        ModBlocks.init();
        ModItems.init();
        ModBlockEntities.init();
        ModMenuTypes.init();
        ModRecipeSerializers.init();
        ModCreativeTabs.init();
    }
}
