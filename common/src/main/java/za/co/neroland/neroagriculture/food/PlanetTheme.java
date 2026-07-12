package za.co.neroland.neroagriculture.food;

import com.mojang.serialization.Codec;

/** Flavour family for engineered/alien crops. EARTH is the standalone default; the rest are Nerospace worlds. */
public enum PlanetTheme {
    EARTH, GREENXERTZ, CINDARA, GLACIRA;

    public static final Codec<PlanetTheme> CODEC = Codec.STRING.xmap(
            name -> PlanetTheme.valueOf(name.toUpperCase(java.util.Locale.ROOT)),
            theme -> theme.name().toLowerCase(java.util.Locale.ROOT));
}
