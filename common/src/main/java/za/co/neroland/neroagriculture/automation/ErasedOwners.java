package za.co.neroland.neroagriculture.automation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.neroagriculture.config.AgricultureConfig;

/**
 * POPIA/GDPR erasure tombstones for owner UUIDs persisted in block-entity NBT. A Core erasure request can
 * only clear owners on <em>loaded</em> block entities; machines in unloaded chunks still carry the UUID in
 * their saved data. This server-scoped {@link SavedData} records each erased UUID (plus the erasure
 * timestamp) so the owner-load path drops it when the chunk eventually loads. Entries are pruned after the
 * configured retention window ({@code privacy.erasure_retention_days}) — long past any plausible chunk
 * revisit — so the tombstone itself is also minimised. UUIDs are never logged.
 */
public final class ErasedOwners extends SavedData {
    public static final Identifier ID = Identifier.fromNamespaceAndPath("neroagriculture", "erased_owners");

    private record Entry(String owner, long erasedAtMillis) {
        static final Codec<Entry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("owner").forGetter(Entry::owner),
                Codec.LONG.fieldOf("erased_at").forGetter(Entry::erasedAtMillis)
        ).apply(instance, Entry::new));
    }

    public static final SavedDataType<ErasedOwners> TYPE = new SavedDataType<>(ID, ErasedOwners::new, codec(), null);

    /** Erased owner UUID → wall-clock erasure time (millis); bounded by the retention prune. */
    private final Map<UUID, Long> erased = new HashMap<>();

    public ErasedOwners() { }

    private static Codec<ErasedOwners> codec() {
        return RecordCodecBuilder.create(instance -> instance.group(
                Entry.CODEC.listOf().fieldOf("erased").forGetter(ErasedOwners::entries)
        ).apply(instance, ErasedOwners::fromEntries));
    }

    private List<Entry> entries() {
        List<Entry> out = new ArrayList<>(erased.size());
        erased.forEach((owner, at) -> out.add(new Entry(owner.toString(), at)));
        out.sort(java.util.Comparator.comparing(Entry::owner));
        return out;
    }

    private static ErasedOwners fromEntries(List<Entry> entries) {
        ErasedOwners data = new ErasedOwners();
        for (Entry entry : entries) {
            try {
                data.erased.put(UUID.fromString(entry.owner()), entry.erasedAtMillis());
            } catch (IllegalArgumentException e) {
                // Malformed tombstone: drop silently (never log the raw value).
            }
        }
        return data;
    }

    /** Server-scoped instance (stored on the overworld); prunes expired tombstones on every access. */
    public static ErasedOwners get(MinecraftServer server) {
        ErasedOwners data = server.overworld().getDataStorage().computeIfAbsent(TYPE);
        data.prune();
        return data;
    }

    /** Record an erasure request for {@code owner} so NBT-persisted copies are dropped on load. */
    public void markErased(UUID owner) {
        erased.put(owner, System.currentTimeMillis());
        setDirty();
    }

    public boolean isErased(UUID owner) {
        return erased.containsKey(owner);
    }

    /** {@code null} when the owner has requested erasure; otherwise {@code owner} unchanged. */
    @Nullable
    public static UUID filter(@Nullable UUID owner, @Nullable MinecraftServer server) {
        if (owner == null || server == null) return owner;
        return get(server).isErased(owner) ? null : owner;
    }

    /** Drop tombstones older than the retention window. */
    public void prune() {
        long cutoff = System.currentTimeMillis()
                - AgricultureConfig.ERASURE_RETENTION_DAYS.get() * 24L * 60L * 60L * 1000L;
        if (erased.entrySet().removeIf(entry -> entry.getValue() < cutoff)) setDirty();
    }
}
