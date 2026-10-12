package za.co.neroland.neroagriculture.link;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.neroagriculture.automation.AutomationOwner;
import za.co.neroland.neroagriculture.lifecycle.ServerStateReset;

/**
 * The one place the link surfaces decide <em>what a client may see</em>, plus the two questions they
 * have to answer before they may say anything: which server is running, and is this player online.
 *
 * <p>Core's snapshot/action API hands over a {@link UUID} and nothing else — no server, no player — so
 * the running server comes from {@code lifecycle/ServerStateReset}, which each loader's server-started
 * hook fills in. Before the first world is loaded there is no server and every section answers empty,
 * which is the honest result rather than a guess.
 *
 * <h2>The visibility rule, in exactly one place</h2>
 *
 * <p>{@link #ownedMachines} is the whole of it: a request sees the loaded machines its own UUID owns,
 * and nothing else. Snapshots, actions and events all route through it, so there is one line to read to
 * know what a companion client can reach — and one line to change if that ever needs to be narrower.
 *
 * <p>Note what it does <b>not</b> do. It never widens for an operator: an operator's powers are a
 * property of a live command source, not of a UUID arriving over a bridge, and a link module that
 * honoured them would turn "I am an admin" into "my phone can read every farm on the server". It never
 * falls back to proximity: {@code getNearestPlayer} answers "who is standing here", which is not the
 * same question as "whose is this" and must never be used to authorise a remote read. And it never
 * force-loads: an unloaded machine is simply absent, because answering a phone is not a reason to load
 * somebody's base.
 *
 * <p>Server thread only. Nothing here reads or stores player data beyond the UUID it is handed.
 */
final class AgricultureLinkAccess {

    private AgricultureLinkAccess() {
    }

    /** The running server, or {@code null} before the first world load / after shutdown. */
    @Nullable
    static MinecraftServer server() {
        return ServerStateReset.currentServer();
    }

    /** The online player with this UUID, or {@code null} if they are not connected. */
    @Nullable
    static ServerPlayer online(MinecraftServer server, UUID playerId) {
        return server.getPlayerList().getPlayer(playerId);
    }

    /**
     * Every loaded block entity of type {@code type} this player owns, in no guaranteed order beyond
     * being stable within one snapshot. The single authorisation seam — see the class notes.
     */
    static <T extends BlockEntity> List<T> ownedMachines(UUID playerId, Class<T> type) {
        List<T> out = new ArrayList<>();
        for (AutomationOwner.Owned owned : AutomationOwner.ownedBy(playerId)) {
            if (type.isInstance(owned)) {
                T machine = type.cast(owned);
                // A removed block entity is still briefly in the tracked set; it is not a machine.
                if (!machine.isRemoved() && machine.getLevel() instanceof ServerLevel) {
                    out.add(machine);
                }
            }
        }
        return out;
    }

    /** The dimension id of a machine's level, or {@code ""} when it somehow has none. */
    static String dimensionOf(BlockEntity machine) {
        return machine.getLevel() instanceof ServerLevel level
                ? level.dimension().identifier().toString() : "";
    }

    /** A machine's own position, as the three fields an app can put on a map. */
    static JsonObject position(BlockPos pos) {
        JsonObject out = new JsonObject();
        out.addProperty("x", pos.getX());
        out.addProperty("y", pos.getY());
        out.addProperty("z", pos.getZ());
        return out;
    }

    /** A stable per-machine key an app can use to correlate rows between polls. */
    static String machineKey(BlockEntity machine) {
        return dimensionOf(machine) + "@" + machine.getBlockPos().asLong();
    }

    /** The fields every payload and every section object starts with. */
    static JsonObject envelope() {
        JsonObject out = new JsonObject();
        out.addProperty("schema_version", AgricultureLinkModule.SCHEMA_VERSION);
        out.addProperty("timestamp", System.currentTimeMillis());
        return out;
    }

    /** One string parameter, trimmed, or {@code null} if it is absent, null or blank. */
    @Nullable
    static String string(@Nullable JsonObject params, String key) {
        if (params == null || !params.has(key)) {
            return null;
        }
        JsonElement element = params.get(key);
        if (element == null || !element.isJsonPrimitive()) {
            return null;
        }
        String raw = element.getAsString();
        return raw == null || raw.isBlank() ? null : raw.trim();
    }

    /**
     * A printable English name derived from a registry path — never from a lang file, which a dedicated
     * server does not have for a mod's assets. {@code fragment_infuser} &rarr; {@code Fragment Infuser}.
     */
    static String readablePath(String path) {
        StringBuilder out = new StringBuilder(path.length());
        boolean capitalise = true;
        for (int i = 0; i < path.length(); i++) {
            char c = path.charAt(i);
            if (c == '_' || c == '/') {
                out.append(' ');
                capitalise = true;
            } else if (capitalise) {
                out.append(Character.toUpperCase(c));
                capitalise = false;
            } else {
                out.append(Character.toLowerCase(c));
            }
        }
        return out.toString().trim().isEmpty() ? path.toLowerCase(Locale.ROOT) : out.toString();
    }
}
