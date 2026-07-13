package za.co.neroland.neroagriculture.machine;

import net.minecraft.resources.Identifier;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.neroagriculture.content.FragmentTier;

/** Central tier-gate and research milestone ids used at start and completion. */
public final class MachineProgression {
    public static final Identifier RESOURCE_RESEARCH = id("resource_seed_researched");
    public static final Identifier FOOD_RESEARCH = id("food_seed_researched");
    public static final Identifier ALIEN_RESEARCH = id("alien_seed_researched");

    private MachineProgression() { }

    @Nullable
    public static Identifier gate(FragmentTier family) {
        // Native standalone gates owned + opened by NeroAgriculture (see AgricultureGates).
        return za.co.neroland.neroagriculture.progression.AgricultureGates.forTier(family);
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath("neroagriculture", path);
    }
}
