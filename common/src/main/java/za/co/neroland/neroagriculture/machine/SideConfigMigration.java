package za.co.neroland.neroagriculture.machine;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import za.co.neroland.nerolandcore.sideconfig.SideConfig;
import za.co.neroland.nerolandcore.sideconfig.SideConfigComponent;
import za.co.neroland.nerolandcore.sideconfig.SideConfigured;

/**
 * One-time reseed of a machine's side configuration when the declared preset changes.
 *
 * <p>Every NeroAgriculture machine used to build its {@link SideConfig} without a
 * {@code defaultPreset(...)}, so Core seeded all six faces of every channel — power
 * included — to {@code DISABLED}. Core's adjacent-energy push always queries the
 * capability with a non-null face, got {@code null} back, and silently skipped the
 * machine; item and fluid faces were equally dead. Declaring
 * {@code SidePreset.PROCESSOR} fixes newly-placed machines, but
 * {@link SideConfig#load} restores the packed NBT, so machines already standing in a
 * world would keep their dead faces forever.
 *
 * <p>The fix is a data version written alongside Core's packed channels: on load, a
 * machine whose stored version predates {@link #VERSION} is reset to its declared
 * preset once. That gate is what stops the reseed from clobbering deliberate face
 * choices made <em>after</em> the fix ships — those saves already carry
 * {@link #VERSION}.
 *
 * <p>Persisting the new version takes an explicit dirty mark, and one that survives
 * the load order. {@code loadAdditional} runs while the block entity's {@code level}
 * is still null — the chunk attaches the level only after the block entity has been
 * deserialised — and {@link BlockEntity#setChanged()} is itself level-guarded, so the
 * {@code owner.setChanged()} that opens Core's {@code SideConfigComponent#markChanged()}
 * does nothing there (the rest of that method is explicitly guarded on the level too).
 * A reseed that happens during load is therefore queued and flushed by {@link #tick} on
 * the machine's first server tick, once the level is attached. Without that the version
 * only landed if something unrelated happened to rewrite the chunk, and an untouched
 * machine re-ran the (idempotent) reseed on every single load.
 *
 * <p>Block data keyed by position: no player identity, nothing personal
 * (POPIA/GDPR — outside the player-erasure scope).
 */
public final class SideConfigMigration {

    /**
     * Current side-config data version.
     *
     * <ul>
     *   <li>{@code 0} (key absent) — pre-fix NBT: every face of every channel DISABLED.</li>
     *   <li>{@code 1} — machines declare {@code SidePreset.PROCESSOR}; reseed once.</li>
     * </ul>
     *
     * <p>Bump this only when a preset change genuinely has to be forced onto machines
     * already placed in a world; every bump costs players their current face layout.
     */
    public static final int VERSION = 1;

    /** NBT key, namespaced like Core's own {@code NeroSideCfg_*} keys to stay collision-free. */
    private static final String VERSION_KEY = "NeroAgSideCfgVersion";

    private SideConfigMigration() {
    }

    /** Whether NBT written at {@code storedVersion} predates the current seeding rules. */
    public static boolean needsReseed(int storedVersion) {
        return storedVersion < VERSION;
    }

    /**
     * Reseed {@code config} from its declared preset when {@code storedVersion} is stale.
     * Idempotent: re-applying the preset to an already-reseeded config yields the same
     * faces, and a config already at {@link #VERSION} is left untouched.
     *
     * @return true if the config was reseeded
     */
    public static boolean apply(SideConfig config, int storedVersion) {
        if (!needsReseed(storedVersion)) {
            return false;
        }
        config.resetToPreset();
        return true;
    }

    /** Call from a machine's {@code loadAdditional}, after {@code super.loadAdditional(input)}. */
    public static void load(SideConfigured machine, ValueInput input) {
        SideConfigComponent component = machine.sideConfig();
        if (component == null) {
            return;
        }
        if (apply(component.config(), input.getIntOr(VERSION_KEY, 0))) {
            component.markChanged();
            markDirty(machine);
        }
    }

    /**
     * Call from a machine's server tick, after Core's shared machine tick. Flushes a reseed that ran
     * before the block entity had a level, so {@link #VERSION} reaches disk instead of waiting on an
     * unrelated write. A no-op — a single volatile read — for every machine that was not reseeded.
     */
    public static void tick(SideConfigured machine) {
        if (!pending || !(machine instanceof BlockEntity blockEntity)) {
            return;
        }
        Level level = blockEntity.getLevel();
        if (level == null || level.isClientSide()) {
            return;
        }
        boolean flush;
        synchronized (PENDING) {
            flush = PENDING.remove(blockEntity);
            pending = !PENDING.isEmpty();
        }
        if (flush) {
            blockEntity.setChanged();
        }
    }

    /** Call from a machine's {@code saveAdditional}, after {@code super.saveAdditional(output)}. */
    public static void save(ValueOutput output) {
        output.putInt(VERSION_KEY, VERSION);
    }

    /**
     * Block entities reseeded before their level was attached, keyed by identity and held weakly so an
     * unloaded chunk can never pin one. Chunk deserialisation runs off the server thread, so arming and
     * flushing are both guarded; {@link #pending} keeps the common (empty) case lock-free.
     */
    private static final Set<BlockEntity> PENDING =
            Collections.newSetFromMap(new WeakHashMap<BlockEntity, Boolean>());
    private static volatile boolean pending;

    private static void markDirty(SideConfigured machine) {
        if (!(machine instanceof BlockEntity blockEntity)) {
            return;
        }
        if (blockEntity.getLevel() != null) {
            blockEntity.setChanged();
            return;
        }
        synchronized (PENDING) {
            PENDING.add(blockEntity);
            pending = true;
        }
    }
}
