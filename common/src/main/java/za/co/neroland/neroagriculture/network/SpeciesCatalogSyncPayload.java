package za.co.neroland.neroagriculture.network;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import za.co.neroland.neroagriculture.NeroAgricultureCommon;
import za.co.neroland.neroagriculture.content.FragmentTier;
import za.co.neroland.neroagriculture.food.FoodDefinition;

/**
 * Minimal bounded display snapshot of the food/alien species catalog; effects, caps, gates, genetics and
 * synthesis rules remain server-only.
 *
 * <p>This payload is catalog metadata only — it carries no player identity and nothing personal
 * (POPIA/GDPR: outside the player-erasure scope).
 */
public record SpeciesCatalogSyncPayload(List<Entry> entries) implements CustomPacketPayload {
    public static final int MAX_ENTRIES = 4096;
    public static final int MAX_PAYLOAD_BYTES = 256 * 1024;
    public static final int MAX_ID_LENGTH = 128;
    public static final int MAX_DISPLAY_LENGTH = 128;
    public static final Type<SpeciesCatalogSyncPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(
            NeroAgricultureCommon.MOD_ID, "species_catalog"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SpeciesCatalogSyncPayload> STREAM_CODEC =
            StreamCodec.of(SpeciesCatalogSyncPayload::write, SpeciesCatalogSyncPayload::read);

    public SpeciesCatalogSyncPayload {
        entries = List.copyOf(entries);
        if (entries.size() > MAX_ENTRIES || estimatedBytes(entries) > MAX_PAYLOAD_BYTES) {
            throw new IllegalArgumentException("Species catalog payload exceeds bounds");
        }
    }

    public static SpeciesCatalogSyncPayload from(Map<Identifier, FoodDefinition> catalog) {
        List<Entry> values = new ArrayList<>();
        int bytes = 5;
        for (FoodDefinition definition : catalog.values()) {
            Entry entry = new Entry(definition.id(), definition.kind(), definition.tier(),
                    definition.displayKey(), definition.color());
            int next = entry.estimatedBytes();
            if (values.size() >= MAX_ENTRIES || bytes + next > MAX_PAYLOAD_BYTES) break;
            values.add(entry);
            bytes += next;
        }
        if (values.size() < catalog.size()) {
            // Counts only — no ids, no player identity (POPIA/GDPR).
            NeroAgricultureCommon.LOGGER.warn(
                    "[NeroAgriculture] Species catalog sync truncated to {} of {} species; clients will not see the rest.",
                    values.size(), catalog.size());
        }
        return new SpeciesCatalogSyncPayload(values);
    }

    private static void write(RegistryFriendlyByteBuf buffer, SpeciesCatalogSyncPayload payload) {
        buffer.writeVarInt(payload.entries.size());
        for (Entry entry : payload.entries) {
            buffer.writeUtf(entry.id.toString(), MAX_ID_LENGTH);
            buffer.writeEnum(entry.kind);
            buffer.writeEnum(entry.tier);
            buffer.writeUtf(entry.displayKey, MAX_DISPLAY_LENGTH);
            buffer.writeInt(entry.color);
        }
    }

    private static SpeciesCatalogSyncPayload read(RegistryFriendlyByteBuf buffer) {
        int count = buffer.readVarInt();
        if (count < 0 || count > MAX_ENTRIES) throw new IllegalArgumentException("Invalid species entry count " + count);
        List<Entry> entries = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            entries.add(new Entry(Identifier.parse(buffer.readUtf(MAX_ID_LENGTH)),
                    buffer.readEnum(FoodDefinition.Kind.class), buffer.readEnum(FragmentTier.class),
                    buffer.readUtf(MAX_DISPLAY_LENGTH), buffer.readInt()));
        }
        return new SpeciesCatalogSyncPayload(entries);
    }

    private static int estimatedBytes(List<Entry> entries) {
        return 5 + entries.stream().mapToInt(Entry::estimatedBytes).sum();
    }

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public record Entry(Identifier id, FoodDefinition.Kind kind, FragmentTier tier, String displayKey, int color) {
        public Entry {
            if (id.toString().length() > MAX_ID_LENGTH || displayKey.isBlank()
                    || displayKey.length() > MAX_DISPLAY_LENGTH || (color & 0xFF000000) != 0) {
                throw new IllegalArgumentException("Invalid client species metadata");
            }
        }
        int estimatedBytes() { return 13 + id.toString().length() * 3 + displayKey.length() * 3; }
    }
}
