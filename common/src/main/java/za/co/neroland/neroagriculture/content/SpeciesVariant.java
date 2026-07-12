package za.co.neroland.neroagriculture.content;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

/** Versioned food/alien species identity stored on the finite seed, crop and produce items. */
public record SpeciesVariant(int version, Identifier species) {
    public static final int CURRENT_VERSION = 1;
    public static final int MAX_ID_LENGTH = 128;

    private static final Codec<SpeciesVariant> RAW_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.intRange(CURRENT_VERSION, CURRENT_VERSION).fieldOf("version").forGetter(SpeciesVariant::version),
            Identifier.CODEC.fieldOf("species").forGetter(SpeciesVariant::species)
    ).apply(instance, SpeciesVariant::new));

    public static final Codec<SpeciesVariant> CODEC = RAW_CODEC.validate(SpeciesVariant::validate);
    public static final StreamCodec<RegistryFriendlyByteBuf, SpeciesVariant> STREAM_CODEC = StreamCodec.of(
            (buffer, value) -> {
                validate(value).getOrThrow();
                buffer.writeVarInt(value.version());
                buffer.writeUtf(value.species().toString(), MAX_ID_LENGTH);
            },
            buffer -> checked(buffer.readVarInt(), Identifier.parse(buffer.readUtf(MAX_ID_LENGTH))));

    public static SpeciesVariant of(Identifier species) {
        return checked(CURRENT_VERSION, species);
    }

    private static SpeciesVariant checked(int version, Identifier species) {
        return validate(new SpeciesVariant(version, species)).getOrThrow();
    }

    private static DataResult<SpeciesVariant> validate(SpeciesVariant value) {
        if (value.version != CURRENT_VERSION) return DataResult.error(() -> "Unsupported species variant version");
        if (value.species == null || value.species.toString().isBlank()) return DataResult.error(() -> "Blank species id");
        if (value.species.toString().length() > MAX_ID_LENGTH) return DataResult.error(() -> "Species id is too long");
        return DataResult.success(value);
    }
}
