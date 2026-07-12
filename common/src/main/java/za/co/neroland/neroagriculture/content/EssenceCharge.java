package za.co.neroland.neroagriculture.content;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

/** Versioned, bounded tier identity carried only by charged seed blanks. */
public record EssenceCharge(int version, EssenceFamily family) {
    public static final int CURRENT_VERSION = 1;
    public static final Codec<EssenceCharge> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.intRange(CURRENT_VERSION, CURRENT_VERSION).fieldOf("version").forGetter(EssenceCharge::version),
            EssenceFamily.CODEC.fieldOf("family").forGetter(EssenceCharge::family)
    ).apply(instance, EssenceCharge::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, EssenceCharge> STREAM_CODEC = StreamCodec.of(
            (buffer, value) -> {
                buffer.writeVarInt(value.version);
                buffer.writeEnum(value.family);
            },
            buffer -> new EssenceCharge(buffer.readVarInt(), buffer.readEnum(EssenceFamily.class)));

    public EssenceCharge {
        if (version != CURRENT_VERSION) throw new IllegalArgumentException("Unsupported charge version");
        if (family == null) throw new IllegalArgumentException("Missing charge family");
    }

    public static EssenceCharge of(EssenceFamily family) {
        return new EssenceCharge(CURRENT_VERSION, family);
    }
}
