package za.co.neroland.neroagriculture.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;

import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;

import org.junit.jupiter.api.Test;

import za.co.neroland.neroagriculture.content.FragmentTier;
import za.co.neroland.neroagriculture.food.FoodDefinition;

class SpeciesCatalogSyncPayloadTest {
    @Test
    void minimalMetadataRoundTrips() {
        SpeciesCatalogSyncPayload payload = new SpeciesCatalogSyncPayload(List.of(
                new SpeciesCatalogSyncPayload.Entry(Identifier.parse("neroagriculture:food/earth_sunfruit"),
                        FoodDefinition.Kind.FOOD, FragmentTier.TERRITE, "food.neroagriculture.earth_sunfruit", 0xE8C34A),
                new SpeciesCatalogSyncPayload.Entry(Identifier.parse("neroagriculture:alien/voidchorus"),
                        FoodDefinition.Kind.ALIEN, FragmentTier.ORBITE, "alien.neroagriculture.voidchorus", 0x9B4FD1)));
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
        SpeciesCatalogSyncPayload.STREAM_CODEC.encode(buffer, payload);
        assertEquals(payload, SpeciesCatalogSyncPayload.STREAM_CODEC.decode(buffer));
        buffer.release();
    }

    @Test
    void rejectsOversizedMetadata() {
        assertThrows(IllegalArgumentException.class, () -> new SpeciesCatalogSyncPayload.Entry(
                Identifier.parse("neroagriculture:food/earth_sunfruit"), FoodDefinition.Kind.FOOD,
                FragmentTier.ORBITE, "x".repeat(129), 0));
    }

    @Test
    void rejectsTooManyEntries() {
        List<SpeciesCatalogSyncPayload.Entry> entries = new ArrayList<>();
        for (int i = 0; i <= SpeciesCatalogSyncPayload.MAX_ENTRIES; i++) {
            entries.add(new SpeciesCatalogSyncPayload.Entry(Identifier.parse("test:s" + i),
                    FoodDefinition.Kind.FOOD, FragmentTier.TERRITE, "s." + i, 0));
        }
        assertThrows(IllegalArgumentException.class, () -> new SpeciesCatalogSyncPayload(entries));
    }

    @Test
    void rejectsOversizedPayload() {
        String displayKey = "d".repeat(SpeciesCatalogSyncPayload.MAX_DISPLAY_LENGTH);
        List<SpeciesCatalogSyncPayload.Entry> entries = new ArrayList<>();
        for (int i = 0; i < 512; i++) {
            entries.add(new SpeciesCatalogSyncPayload.Entry(
                    Identifier.parse("test:" + "a".repeat(100) + i), FoodDefinition.Kind.ALIEN,
                    FragmentTier.VOIDITE, displayKey, 0));
        }
        assertThrows(IllegalArgumentException.class, () -> new SpeciesCatalogSyncPayload(entries));
    }

    @Test
    void byteEstimateCoversIdKindTierDisplayAndColour() {
        Identifier id = Identifier.parse("neroagriculture:food/earth_sunfruit");
        String displayKey = "food.neroagriculture.earth_sunfruit";
        SpeciesCatalogSyncPayload.Entry entry = new SpeciesCatalogSyncPayload.Entry(id, FoodDefinition.Kind.FOOD,
                FragmentTier.TERRITE, displayKey, 0xE8C34A);
        assertEquals(13 + id.toString().length() * 3 + displayKey.length() * 3, entry.estimatedBytes());
    }
}
