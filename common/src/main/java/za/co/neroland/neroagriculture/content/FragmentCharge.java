package za.co.neroland.neroagriculture.content;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

/** Versioned, bounded tier identity carried only by charged seed blanks. */
public record FragmentCharge(int version, FragmentTier family) {
    public static final int CURRENT_VERSION = 1;
    public static final Codec<FragmentCharge> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.intRange(CURRENT_VERSION, CURRENT_VERSION).fieldOf("version").forGetter(FragmentCharge::version),
            FragmentTier.CODEC.fieldOf("family").forGetter(FragmentCharge::family)
    ).apply(instance, FragmentCharge::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, FragmentCharge> STREAM_CODEC = StreamCodec.of(
            (buffer, value) -> {
                buffer.writeVarInt(value.version);
                buffer.writeEnum(value.family);
            },
            buffer -> new FragmentCharge(buffer.readVarInt(), buffer.readEnum(FragmentTier.class)));

    public FragmentCharge {
        if (version != CURRENT_VERSION) throw new IllegalArgumentException("Unsupported charge version");
        if (family == null) throw new IllegalArgumentException("Missing charge family");
    }

    public static FragmentCharge of(FragmentTier family) {
        return new FragmentCharge(CURRENT_VERSION, family);
    }
}
