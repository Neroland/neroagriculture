package za.co.neroland.neroagriculture.content;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

/** Versioned dynamic material identity stored on finite registered seed/fragment items. */
public record MaterialVariant(int version, Identifier material, FragmentTier family) {
    public static final int CURRENT_VERSION = 1;
    public static final int MAX_ID_LENGTH = 128;

    private static final Codec<MaterialVariant> RAW_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.intRange(CURRENT_VERSION, CURRENT_VERSION).fieldOf("version").forGetter(MaterialVariant::version),
            Identifier.CODEC.fieldOf("material").forGetter(MaterialVariant::material),
            FragmentTier.CODEC.fieldOf("family").forGetter(MaterialVariant::family)
    ).apply(instance, MaterialVariant::new));

    public static final Codec<MaterialVariant> CODEC = RAW_CODEC.validate(MaterialVariant::validate);
    public static final StreamCodec<RegistryFriendlyByteBuf, MaterialVariant> STREAM_CODEC = StreamCodec.of(
            (buffer, value) -> {
                validate(value).getOrThrow();
                buffer.writeVarInt(value.version());
                buffer.writeUtf(value.material().toString(), MAX_ID_LENGTH);
                buffer.writeEnum(value.family());
            },
            buffer -> checked(buffer.readVarInt(), Identifier.parse(buffer.readUtf(MAX_ID_LENGTH)),
                    buffer.readEnum(FragmentTier.class)));

    public static MaterialVariant of(Identifier material, FragmentTier family) {
        return checked(CURRENT_VERSION, material, family);
    }

    private static MaterialVariant checked(int version, Identifier material, FragmentTier family) {
        MaterialVariant value = new MaterialVariant(version, material, family);
        return validate(value).getOrThrow();
    }

    private static DataResult<MaterialVariant> validate(MaterialVariant value) {
        if (value.version != CURRENT_VERSION) return DataResult.error(() -> "Unsupported material variant version");
        if (value.material == null || value.material.toString().isBlank()) return DataResult.error(() -> "Blank material id");
        if (value.material.toString().length() > MAX_ID_LENGTH) return DataResult.error(() -> "Material id is too long");
        if (value.family == null) return DataResult.error(() -> "Missing fragment family");
        return DataResult.success(value);
    }
}
