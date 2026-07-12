package za.co.neroland.neroagriculture.food;

/**
 * Pure gate shared by the fabrication path and tests: a species may only be synthesized when it is a
 * researched, non-natural (derived) strain. Natural alien strains are found, never synthesized.
 */
public final class FoodSynthesis {
    private FoodSynthesis() { }

    public static boolean canSynthesize(FoodDefinition definition, boolean researched) {
        return definition != null && definition.synthesizable() && researched;
    }
}
