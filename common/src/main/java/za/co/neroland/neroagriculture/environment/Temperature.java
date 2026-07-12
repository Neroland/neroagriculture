package za.co.neroland.neroagriculture.environment;

import com.mojang.serialization.Codec;

/** Coarse growth temperature band. TEMPERATE is the habitable default. */
public enum Temperature {
    COLD, TEMPERATE, HOT;

    public static final Codec<Temperature> CODEC = Codec.STRING.xmap(
            name -> Temperature.valueOf(name.toUpperCase(java.util.Locale.ROOT)),
            temperature -> temperature.name().toLowerCase(java.util.Locale.ROOT));
}
