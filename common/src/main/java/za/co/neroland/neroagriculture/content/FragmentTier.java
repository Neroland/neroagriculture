package za.co.neroland.neroagriculture.content;

import com.mojang.serialization.Codec;

/** Stable neutral affinity used by seeds, fragments and grow beds. */
public enum FragmentTier {
    TERRITE, FORGITE, ORBITE, COLONITE, VOIDITE;

    public static final Codec<FragmentTier> CODEC = Codec.STRING.xmap(
            name -> FragmentTier.valueOf(name.toUpperCase(java.util.Locale.ROOT)),
            family -> family.name().toLowerCase(java.util.Locale.ROOT));
}
