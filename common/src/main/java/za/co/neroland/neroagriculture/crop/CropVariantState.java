package za.co.neroland.neroagriculture.crop;

import java.util.Objects;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.resources.Identifier;
import za.co.neroland.neroagriculture.content.FragmentTier;

/** Immutable save-facing crop identity. Material ids are never replaced with numeric/catalog indices. */
public record CropVariantState(int formatVersion, Identifier material, FragmentTier family, int harvestCount) {
    public static final int CURRENT_FORMAT = 1;
    public static final int MAX_HARVEST_COUNT = 1_000_000_000;
    public static final Codec<CropVariantState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.intRange(CURRENT_FORMAT, CURRENT_FORMAT).fieldOf("format").forGetter(CropVariantState::formatVersion),
            Identifier.CODEC.fieldOf("material").forGetter(CropVariantState::material),
            FragmentTier.CODEC.fieldOf("family").forGetter(CropVariantState::family),
            Codec.intRange(0, MAX_HARVEST_COUNT).fieldOf("harvest_count").forGetter(CropVariantState::harvestCount)
    ).apply(instance, CropVariantState::new));

    public CropVariantState {
        Objects.requireNonNull(material, "material");
        Objects.requireNonNull(family, "family");
        if (formatVersion != CURRENT_FORMAT) throw new IllegalArgumentException("Unsupported crop format " + formatVersion);
        if (harvestCount < 0 || harvestCount > MAX_HARVEST_COUNT) throw new IllegalArgumentException("Invalid harvest count");
    }

    public static CropVariantState fresh(Identifier material) { return fresh(material, FragmentTier.ORBITE); }
    public static CropVariantState fresh(Identifier material, FragmentTier family) { return new CropVariantState(CURRENT_FORMAT, material, family, 0); }
    public CropVariantState harvested() { return new CropVariantState(formatVersion, material, family, Math.min(MAX_HARVEST_COUNT, harvestCount + 1)); }
}
