package za.co.neroland.neroagriculture.machine;

import net.minecraft.resources.Identifier;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.neroagriculture.content.EssenceFamily;

/** Central tier-gate and research milestone ids used at start and completion. */
public final class MachineProgression {
    public static final Identifier RESOURCE_RESEARCH = id("resource_seed_researched");
    public static final Identifier FOOD_RESEARCH = id("food_seed_researched");
    public static final Identifier ALIEN_RESEARCH = id("alien_seed_researched");

    private MachineProgression() { }

    @Nullable
    public static Identifier gate(EssenceFamily family) {
        return switch (family) {
            case TERRAN -> null;
            case INDUSTRIAL -> Identifier.parse("nerolandcore:industrial_power");
            case ORBITAL -> Identifier.parse("nerolandcore:reached_orbit");
            case COLONIAL -> Identifier.parse("nerolandcore:first_colony");
            case DEEPVOID -> Identifier.parse("nerolandcore:deep_space");
        };
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath("neroagriculture", path);
    }
}
