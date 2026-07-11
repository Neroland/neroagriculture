package za.co.neroland.neroagriculture.content;

import com.mojang.serialization.Codec;

/** Stable neutral affinity used by seeds, essences and grow beds. */
public enum EssenceFamily {
    TERRAN, INDUSTRIAL, ORBITAL, COLONIAL, DEEPVOID;

    public static final Codec<EssenceFamily> CODEC = Codec.STRING.xmap(
            name -> EssenceFamily.valueOf(name.toUpperCase(java.util.Locale.ROOT)),
            family -> family.name().toLowerCase(java.util.Locale.ROOT));
}
