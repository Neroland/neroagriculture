package za.co.neroland.neroagriculture.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;

import org.junit.jupiter.api.Test;

import za.co.neroland.neroagriculture.content.EssenceFamily;

class MaterialCatalogSyncPayloadTest {
    @Test
    void minimalMetadataRoundTrips() {
        MaterialCatalogSyncPayload payload = new MaterialCatalogSyncPayload(List.of(
                new MaterialCatalogSyncPayload.Entry(Identifier.parse("test:iron"), EssenceFamily.INDUSTRIAL,
                        "material.test.iron", 0xAABBCC)));
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
        MaterialCatalogSyncPayload.STREAM_CODEC.encode(buffer, payload);
        assertEquals(payload, MaterialCatalogSyncPayload.STREAM_CODEC.decode(buffer));
        buffer.release();
    }

    @Test
    void rejectsOversizedMetadata() {
        assertThrows(IllegalArgumentException.class, () -> new MaterialCatalogSyncPayload.Entry(
                Identifier.parse("test:iron"), EssenceFamily.ORBITAL, "x".repeat(129), 0));
    }
}
