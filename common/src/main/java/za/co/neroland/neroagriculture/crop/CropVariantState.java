package za.co.neroland.neroagriculture.crop;

import java.util.Objects;

import net.minecraft.resources.Identifier;

/** Immutable save-facing crop identity. Material ids are never replaced with numeric/catalog indices. */
public record CropVariantState(int formatVersion, Identifier material, int harvestCount) {
    public static final int CURRENT_FORMAT = 1;
    public static final int MAX_HARVEST_COUNT = 1_000_000_000;

    public CropVariantState {
        Objects.requireNonNull(material, "material");
        if (formatVersion != CURRENT_FORMAT) throw new IllegalArgumentException("Unsupported crop format " + formatVersion);
        if (harvestCount < 0 || harvestCount > MAX_HARVEST_COUNT) throw new IllegalArgumentException("Invalid harvest count");
    }

    public static CropVariantState fresh(Identifier material) { return new CropVariantState(CURRENT_FORMAT, material, 0); }
    public CropVariantState harvested() { return new CropVariantState(formatVersion, material, Math.min(MAX_HARVEST_COUNT, harvestCount + 1)); }
}
