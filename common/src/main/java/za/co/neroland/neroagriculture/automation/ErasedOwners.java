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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.neroagriculture.config.AgricultureConfig;
import za.co.neroland.nerolandcore.data.SavedDataRecovery;

/**
 * POPIA/GDPR erasure tombstones for owner UUIDs persisted in block-entity NBT. A Core erasure request can
 * only clear owners on <em>loaded</em> block entities; machines in unloaded chunks still carry the UUID in
 * their saved data. This server-scoped {@link SavedData} records each erased UUID (plus the erasure
 * timestamp) so the owner-load path drops it when the chunk eventually loads. Entries are pruned after the
 * configured retention window ({@code privacy.erasure_retention_days}) — long past any plausible chunk
 * revisit — so the tombstone itself is also minimised. UUIDs are never logged.
 *
 * <h2>Crash safety is an erasure requirement here, not just a stability one</h2>
 *
 * <p>{@link #filter} runs on <em>every owned block-entity load</em>, so this store is fetched once per
 * chunk load for as long as the world is played. An unreadable {@code erased_owners.dat} read straight
 * through {@code computeIfAbsent} would therefore throw on every one of those loads — a repeating hard
 * crash — <em>and</em> stop erased owners from being dropped, which turns a disk fault into a failed
 * erasure request. Every fetch consequently goes through
 * {@link SavedDataRecovery#get(ServerLevel, SavedDataType, java.util.function.Supplier, String)}, which
 * falls back to a last-known-good backup and then to a fresh store.
 *
 * <p>That backup is a second copy of the same UUID rows, so it is kept in step with the primary
 * deliberately: {@link #eraseFor} forces a backup immediately after recording a tombstone (the tombstone
 * must survive to do its job) and {@link #get} forces one immediately after a retention prune actually
 * removes rows (an expired tombstone must not linger in the backup past its retention window).
 */
public final class ErasedOwners extends SavedData {
    public static final Identifier ID = Identifier.fromNamespaceAndPath("neroagriculture", "erased_owners");

    /** Stable, non-identifying label for recovery logs, telemetry and the backup file name. */
    private static final String STORE = "neroagriculture:erased_owners";

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

    /**
     * Server-scoped instance (stored on the overworld); prunes expired tombstones on every access.
     *
     * <p>Loaded through {@link SavedDataRecovery} rather than {@code computeIfAbsent}: this is the
     * chunk-load path, so a corrupt file here is a repeating crash <em>and</em> a silently failed
     * erasure. When the prune actually drops rows the backup is refreshed at once, so an expired
     * tombstone never outlives its retention window in the backup copy.
     */
    public static ErasedOwners get(MinecraftServer server) {
        ServerLevel overworld = server.overworld();
        ErasedOwners data = SavedDataRecovery.get(overworld, TYPE, ErasedOwners::new, STORE);
        if (data.prune()) {
            SavedDataRecovery.backupNow(overworld, TYPE, data, STORE);
        }
        return data;
    }

    /**
     * Records an erasure request for {@code owner} so NBT-persisted copies are dropped on load, and
     * pushes the tombstone to the recovery backup in the same call — a tombstone that only exists in the
     * primary file would be lost by exactly the recovery path that is meant to keep erasure working.
     *
     * <p>This is the entry point {@link AutomationOwner}'s Core eraser uses; {@link #markErased} remains
     * for callers that already hold the store.
     */
    public static void eraseFor(MinecraftServer server, UUID owner) {
        ServerLevel overworld = server.overworld();
        ErasedOwners data = get(server);
        data.markErased(owner);
        SavedDataRecovery.backupNow(overworld, TYPE, data, STORE);
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

    /** Drop tombstones older than the retention window; true when something was actually removed. */
    public boolean prune() {
        long cutoff = System.currentTimeMillis()
                - AgricultureConfig.ERASURE_RETENTION_DAYS.get() * 24L * 60L * 60L * 1000L;
        if (!erased.entrySet().removeIf(entry -> entry.getValue() < cutoff)) return false;
        setDirty();
        return true;
    }
}
