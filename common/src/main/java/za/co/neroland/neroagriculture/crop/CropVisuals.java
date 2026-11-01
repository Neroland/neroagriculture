package za.co.neroland.neroagriculture.crop;

import net.minecraft.resources.Identifier;

/** Small, fixed model families keep crop blockstates bounded for dynamic material catalogs. */
public final class CropVisuals {
    public static final int GENERIC = 0;
    public static final int METAL = 1;
    public static final int CRYSTAL = 2;
    public static final int CARBON = 3;

    private CropVisuals() { }

    public static String name(int family) {
        return switch (family) {
            case METAL -> "metal";
            case CRYSTAL -> "crystal";
            case CARBON -> "carbon";
            default -> "generic";
        };
    }

    public static int parse(String value) {
        return switch (value) {
            case "generic" -> GENERIC;
            case "metal" -> METAL;
            case "crystal" -> CRYSTAL;
            case "carbon" -> CARBON;
            default -> throw new IllegalArgumentException("unknown visual_family '" + value + "'");
        };
    }

    public static int resolve(net.minecraft.world.level.Level level, Identifier material) {
        if (level.getServer() == null) return family(material);
        return za.co.neroland.neroagriculture.catalog.MaterialCatalog.forServer(level.getServer()).lookup(material)
                .material().map(value -> value.definition().visualFamily()).orElseGet(() -> family(material));
    }

    public static int family(Identifier material) {
        return switch (material.toString()) {
            case "c:iron", "c:copper", "c:gold", "nerospace:raw_nerosium", "nerospace:raw_nerosteel" -> METAL;
            case "c:diamond", "c:redstone", "nerospace:xertz_quartz", "nerospace:glacite" -> CRYSTAL;
            case "c:coal", "nerospace:cindrite" -> CARBON;
            default -> GENERIC;
        };
    }
}
