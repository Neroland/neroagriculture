package za.co.neroland.neroagriculture.automation;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.neroagriculture.config.AgricultureConfig;
import za.co.neroland.nerolandcore.data.PlayerDataErasure;

/**
 * POPIA/GDPR-safe owner tracking for automation machines. Only a bare UUID is stored (never a name), the
 * feature is opt-out via config, and a Core {@link PlayerDataErasure} eraser clears the owner from any
 * loaded owned machine when a player requests erasure. Owners persisted in unloaded-chunk NBT are dropped
 * on load through the {@link ErasedOwners} tombstone set the same eraser maintains.
 */
public final class AutomationOwner {
    /** Implemented by automation block entities that hold an owner UUID. */
    public interface Owned {
        @Nullable UUID automationOwner();
        void clearAutomationOwner();
    }

    private static final Set<Owned> TRACKED = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private AutomationOwner() { }

    public static void register() {
        PlayerDataErasure.register((server, uuid) -> {
            // Tombstone first so machines saved in unloaded chunks drop the owner when they load.
            ErasedOwners.get(server).markErased(uuid);
            for (Owned owned : TRACKED) {
                if (uuid.equals(owned.automationOwner())) owned.clearAutomationOwner();
            }
        });
    }

    /**
     * Owner-first progression resolution: when a machine has a recorded owner, gates and milestones are
     * checked against (and credited to) that player only — {@code null} while the owner is offline, so
     * progression fails closed rather than crediting a bystander. Only an <em>ownerless</em> machine
     * (owner tracking opted out, or pre-owner worlds) falls back to the nearest player, so the opt-out
     * never bricks progression.
     */
    @Nullable
    public static ServerPlayer gatePlayer(ServerLevel level, BlockPos pos, @Nullable UUID owner,
            double fallbackRange) {
        if (owner != null) return level.getServer().getPlayerList().getPlayer(owner);
        Player nearest = level.getNearestPlayer(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                fallbackRange, false);
        return nearest instanceof ServerPlayer serverPlayer ? serverPlayer : null;
    }

    public static boolean trackingEnabled() {
        return AgricultureConfig.AUTOMATION_TRACK_OWNER.get();
    }

    public static void track(Owned owned) { TRACKED.add(owned); }
    public static void untrack(Owned owned) { TRACKED.remove(owned); }

    public static void save(ValueOutput output, @Nullable UUID owner) {
        if (owner != null && trackingEnabled()) output.putString("Owner", owner.toString());
    }

    @Nullable
    public static UUID load(ValueInput input) {
        String raw = input.getStringOr("Owner", "");
        if (raw.isBlank()) return null;
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
