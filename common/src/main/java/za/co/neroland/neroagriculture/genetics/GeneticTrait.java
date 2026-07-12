package za.co.neroland.neroagriculture.genetics;

import com.mojang.serialization.Codec;

/** The five capped seed traits. Each ranges 0-5; the sum across all traits is capped at 15. */
public enum GeneticTrait {
    YIELD, SPEED, HARDINESS, OXYGEN_OUTPUT, FOOD_POTENCY;

    public static final int MAX_PER_TRAIT = 5;
    public static final int TOTAL_CAP = 15;

    public static final Codec<GeneticTrait> CODEC = Codec.STRING.xmap(
            name -> GeneticTrait.valueOf(name.toUpperCase(java.util.Locale.ROOT)),
            trait -> trait.name().toLowerCase(java.util.Locale.ROOT));
}
