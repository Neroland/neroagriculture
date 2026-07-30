package za.co.neroland.neroagriculture.content;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.mojang.serialization.JsonOps;

import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;

import org.junit.jupiter.api.Test;

class MaterialVariantTest {
    @Test
    void persistentCodecRoundTrips() {
        MaterialVariant value = MaterialVariant.of(Identifier.parse("nerospace:nerosium_ingot"), FragmentTier.ORBITE);
        var json = MaterialVariant.CODEC.encodeStart(JsonOps.INSTANCE, value).getOrThrow();
        assertEquals(value, MaterialVariant.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow());
    }

    @Test
    void streamCodecRoundTrips() {
        MaterialVariant value = MaterialVariant.of(Identifier.parse("minecraft:iron_ingot"), FragmentTier.FORGITE);
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
        MaterialVariant.STREAM_CODEC.encode(buffer, value);
        assertEquals(value, MaterialVariant.STREAM_CODEC.decode(buffer));
        buffer.release();
    }

    @Test
    void malformedAndOversizedIdsAreRejected() {
        assertThrows(RuntimeException.class, () -> Identifier.parse("not an id"));
        String oversized = "test:" + "a".repeat(MaterialVariant.MAX_ID_LENGTH);
        assertThrows(IllegalStateException.class,
                () -> MaterialVariant.of(Identifier.parse(oversized), FragmentTier.TERRITE));
    }
}
