package za.co.neroland.neroagriculture.food;

import com.mojang.serialization.Codec;

/**
 * The six documented signature-effect categories. Three map to vanilla effects; three are
 * Agriculture-owned (resolved by {@link FoodEffects}). NONE lets a food be pure nutrition.
 */
public enum EffectCategory {
    NONE(false),
    NIGHT_VISION(false),
    MINING_HASTE(false),
    FIRE_RESISTANCE(false),
    LOW_GRAVITY_ADAPTATION(true),
    OXYGEN_EFFICIENCY(true),
    FREEZE_IMMUNITY(true);

    private final boolean custom;

    EffectCategory(boolean custom) {
        this.custom = custom;
    }

    /** True when this category resolves to an Agriculture-registered effect rather than a vanilla one. */
    public boolean custom() {
        return custom;
    }

    public static final Codec<EffectCategory> CODEC = Codec.STRING.xmap(
            name -> EffectCategory.valueOf(name.toUpperCase(java.util.Locale.ROOT)),
            category -> category.name().toLowerCase(java.util.Locale.ROOT));
}
