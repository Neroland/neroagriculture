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

/** Bounded display metadata; recipe inputs and progression state remain server-authoritative. */
public record MaterialCatalogSyncPayload(List<Entry> entries) implements CustomPacketPayload {
    public static final int MAX_ENTRIES = 4096;
    public static final int MAX_PAYLOAD_BYTES = 256 * 1024;
    public static final int MAX_ID_LENGTH = 128;
    public static final int MAX_DISPLAY_LENGTH = 128;
    public static final Type<MaterialCatalogSyncPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(
            NeroAgricultureCommon.MOD_ID, "material_catalog_v2"));
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
            Entry entry = new Entry(definition.id(), definition.tier(), definition.displayKey(), definition.color(), definition.visualFamily(),
                    definition.yield().minimum(), definition.yield().maximum(), definition.yield().rampHarvests(),
                    definition.conversion(), definition.gate() == null ? "" : definition.gate().toString(),
                    definition.worldRestriction() == null ? "" : definition.worldRestriction().dimension().toString());
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
            buffer.writeVarInt(entry.visualFamily);
            buffer.writeVarInt(entry.minimumYield);
            buffer.writeVarInt(entry.maximumYield);
            buffer.writeVarInt(entry.rampHarvests);
            buffer.writeVarInt(entry.conversion);
            buffer.writeUtf(entry.gate, MAX_ID_LENGTH);
            buffer.writeUtf(entry.dimension, MAX_ID_LENGTH);
        }
    }

    private static MaterialCatalogSyncPayload read(RegistryFriendlyByteBuf buffer) {
        int count = buffer.readVarInt();
        if (count < 0 || count > MAX_ENTRIES) throw new IllegalArgumentException("Invalid catalog entry count " + count);
        List<Entry> entries = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            entries.add(new Entry(Identifier.parse(buffer.readUtf(MAX_ID_LENGTH)),
                    buffer.readEnum(FragmentTier.class), buffer.readUtf(MAX_DISPLAY_LENGTH), buffer.readInt(), buffer.readVarInt(),
                    buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(),
                    buffer.readUtf(MAX_ID_LENGTH), buffer.readUtf(MAX_ID_LENGTH)));
        }
        return new MaterialCatalogSyncPayload(entries);
    }

    private static int estimatedBytes(List<Entry> entries) {
        return 5 + entries.stream().mapToInt(Entry::estimatedBytes).sum();
    }

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public record Entry(Identifier id, FragmentTier tier, String displayKey, int color, int visualFamily,
            int minimumYield, int maximumYield, int rampHarvests, int conversion, String gate, String dimension) {
        public Entry {
            if (id.toString().length() > MAX_ID_LENGTH || displayKey.isBlank()
                    || displayKey.length() > MAX_DISPLAY_LENGTH || (color & 0xFF000000) != 0 || visualFamily < 0 || visualFamily > 3
                    || minimumYield < 0 || maximumYield < minimumYield || maximumYield > 4096
                    || rampHarvests < 0 || rampHarvests > 1_000_000 || conversion < 1 || conversion > 1_000_000
                    || gate.length() > MAX_ID_LENGTH || dimension.length() > MAX_ID_LENGTH) {
                throw new IllegalArgumentException("Invalid client catalog metadata");
            }
        }
        int estimatedBytes() {
            return 48 + (id.toString().length() + displayKey.length() + gate.length() + dimension.length()) * 3;
        }
    }
}
