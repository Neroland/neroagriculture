package za.co.neroland.neroagriculture.network;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import za.co.neroland.neroagriculture.NeroAgricultureCommon;
import za.co.neroland.neroagriculture.catalog.ResolvedCatalog;
import za.co.neroland.neroagriculture.content.FragmentTier;

/** Minimal bounded display snapshot; recipes, gates, selectors and conversion rules remain server-only. */
public record MaterialCatalogSyncPayload(List<Entry> entries) implements CustomPacketPayload {
    public static final int MAX_ENTRIES = 4096;
    public static final int MAX_PAYLOAD_BYTES = 256 * 1024;
    public static final int MAX_ID_LENGTH = 128;
    public static final int MAX_DISPLAY_LENGTH = 128;
    public static final Type<MaterialCatalogSyncPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(
            NeroAgricultureCommon.MOD_ID, "material_catalog"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MaterialCatalogSyncPayload> STREAM_CODEC =
            StreamCodec.of(MaterialCatalogSyncPayload::write, MaterialCatalogSyncPayload::read);

    public MaterialCatalogSyncPayload {
        entries = List.copyOf(entries);
        if (entries.size() > MAX_ENTRIES || estimatedBytes(entries) > MAX_PAYLOAD_BYTES) {
            throw new IllegalArgumentException("Material catalog payload exceeds bounds");
        }
    }

    public static MaterialCatalogSyncPayload from(ResolvedCatalog catalog) {
        List<Entry> values = new ArrayList<>();
        int bytes = 5;
        for (var resolved : catalog.exposed().values()) {
            var definition = resolved.definition();
            Entry entry = new Entry(definition.id(), definition.tier(), definition.displayKey(), definition.color());
            int next = entry.estimatedBytes();
            if (values.size() >= MAX_ENTRIES || bytes + next > MAX_PAYLOAD_BYTES) break;
            values.add(entry);
            bytes += next;
        }
        if (values.size() < catalog.exposed().size()) {
            // Counts only — no ids, no player identity (POPIA/GDPR).
            NeroAgricultureCommon.LOGGER.warn(
                    "[NeroAgriculture] Material catalog sync truncated to {} of {} materials; clients will not see the rest.",
                    values.size(), catalog.exposed().size());
        }
        return new MaterialCatalogSyncPayload(values);
    }

    private static void write(RegistryFriendlyByteBuf buffer, MaterialCatalogSyncPayload payload) {
        buffer.writeVarInt(payload.entries.size());
        for (Entry entry : payload.entries) {
            buffer.writeUtf(entry.id.toString(), MAX_ID_LENGTH);
            buffer.writeEnum(entry.tier);
            buffer.writeUtf(entry.displayKey, MAX_DISPLAY_LENGTH);
            buffer.writeInt(entry.color);
        }
    }

    private static MaterialCatalogSyncPayload read(RegistryFriendlyByteBuf buffer) {
        int count = buffer.readVarInt();
        if (count < 0 || count > MAX_ENTRIES) throw new IllegalArgumentException("Invalid catalog entry count " + count);
        List<Entry> entries = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            entries.add(new Entry(Identifier.parse(buffer.readUtf(MAX_ID_LENGTH)),
                    buffer.readEnum(FragmentTier.class), buffer.readUtf(MAX_DISPLAY_LENGTH), buffer.readInt()));
        }
        return new MaterialCatalogSyncPayload(entries);
    }

    private static int estimatedBytes(List<Entry> entries) {
        return 5 + entries.stream().mapToInt(Entry::estimatedBytes).sum();
    }

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public record Entry(Identifier id, FragmentTier tier, String displayKey, int color) {
        public Entry {
            if (id.toString().length() > MAX_ID_LENGTH || displayKey.isBlank()
                    || displayKey.length() > MAX_DISPLAY_LENGTH || (color & 0xFF000000) != 0) {
                throw new IllegalArgumentException("Invalid client catalog metadata");
            }
        }
        int estimatedBytes() { return 12 + id.toString().length() * 3 + displayKey.length() * 3; }
    }
}
