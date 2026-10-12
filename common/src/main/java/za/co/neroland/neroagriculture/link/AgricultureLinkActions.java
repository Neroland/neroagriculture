package za.co.neroland.neroagriculture.link;

import java.util.List;
import java.util.UUID;

import com.google.gson.JsonObject;

import net.minecraft.server.MinecraftServer;

import za.co.neroland.nerolandcore.link.LinkActionHandler;
import za.co.neroland.nerolandcore.link.LinkActionResult;
import za.co.neroland.nerolandcore.link.LinkAlerts;

import za.co.neroland.neroagriculture.NeroAgricultureCommon;
import za.co.neroland.neroagriculture.config.AgricultureConfig;

/**
 * The write half of the link module, and a deliberately minimal one: a single action,
 * {@code acknowledge_alert}, which touches nothing but the requesting player's own notification list.
 *
 * <h2>Why nothing else</h2>
 *
 * <p>Everything a companion client might plausibly want to do to a farm — plant a seed, harvest a bed,
 * apply fertiliser, start or roll back a terraforming project, move items between a machine's slots —
 * either creates items, consumes items, or changes the world. A farm is an item factory; every write
 * worth having is a write that produces or spends something, which is precisely the class of action
 * that should require being in the world rather than holding a phone. There is no NeroAgriculture
 * equivalent of NeroColonies' {@code toggle_export}: no routing switch that changes a destination
 * without changing a quantity.
 *
 * <p>The one candidate that changes no quantity at all is the area machines' working-area hologram
 * toggle, and it is left out on cost/benefit rather than principle: it is a cosmetic particle effect
 * visible only to somebody already standing next to the machine, so remote control of it is of no use
 * to the person holding the phone.
 *
 * <p>Acknowledging your own alert is the exception that proves the rule — it moves a flag in Core's
 * per-player alert store, touches no world state, and is exactly the thing you want to do from a phone:
 * dismiss the notification that woke you.
 *
 * <h2>Validation</h2>
 *
 * <p>Server-authoritative, and the incoming {@link UUID} is trusted for <em>nothing beyond scoping the
 * request to that player's own alerts</em>. {@code LinkAlerts.ack} is itself keyed by player, so an
 * alert id belonging to somebody else simply does not resolve and the call answers
 * {@link LinkActionResult.Error#VALIDATION} — the same answer as a typo, so the action cannot be used
 * to probe for other players' alerts.
 *
 * <p><b>Privacy (POPIA/GDPR).</b> No coordinates are read or written, and no result names any player.
 *
 * <p>Server thread only.
 */
public final class AgricultureLinkActions implements LinkActionHandler {

    private static final List<String> ACTIONS = List.of(AgricultureLinkModule.ACTION_ACKNOWLEDGE_ALERT);

    @Override
    public String moduleId() {
        return AgricultureLinkModule.MODULE_ID;
    }

    @Override
    public List<String> actionIds() {
        return ACTIONS;
    }

    /**
     * Acknowledging your own alert is a notification-list operation and works perfectly well while you
     * are away — that is rather the point of an alert.
     */
    @Override
    public boolean allowOffline(String actionId) {
        return AgricultureLinkModule.ACTION_ACKNOWLEDGE_ALERT.equals(actionId);
    }

    @Override
    public LinkActionResult execute(UUID playerId, String actionId, JsonObject params) {
        if (playerId == null) {
            return LinkActionResult.error(LinkActionResult.Error.VALIDATION, "No player was supplied.");
        }
        if (!AgricultureConfig.LINK_MODULE_ENABLED.get()) {
            return LinkActionResult.error(LinkActionResult.Error.ACTION_DISABLED,
                    "The NeroAgriculture link module is disabled on this server.");
        }
        MinecraftServer server = AgricultureLinkAccess.server();
        if (server == null) {
            return LinkActionResult.error(LinkActionResult.Error.INTERNAL,
                    "The server is not running a world yet.");
        }
        try {
            if (AgricultureLinkModule.ACTION_ACKNOWLEDGE_ALERT.equals(actionId)) {
                return acknowledgeAlert(server, playerId, params);
            }
            return LinkActionResult.error(LinkActionResult.Error.VALIDATION,
                    "NeroAgriculture does not know the action '" + actionId + "'.");
        } catch (RuntimeException e) {
            // Action id only — never who it was for (POPIA/GDPR).
            NeroAgricultureCommon.LOGGER.warn(
                    "[NeroAgriculture] The NeroLink action '{}' failed.", actionId, e);
            return LinkActionResult.error(LinkActionResult.Error.INTERNAL,
                    "NeroAgriculture could not complete that action.");
        }
    }

    /** Marks one of this player's own NeroAgriculture alerts acknowledged in Core's store. */
    private LinkActionResult acknowledgeAlert(MinecraftServer server, UUID playerId, JsonObject params) {
        String alertId = AgricultureLinkAccess.string(params, "alert");
        if (alertId == null) {
            return LinkActionResult.error(LinkActionResult.Error.VALIDATION,
                    "An 'alert' id is required.");
        }
        // Keyed by player inside Core, so "not yours" and "does not exist" give the same answer.
        if (!LinkAlerts.get(server).ack(server, playerId, alertId)) {
            return LinkActionResult.error(LinkActionResult.Error.VALIDATION,
                    "No such alert.");
        }
        JsonObject state = AgricultureLinkAccess.envelope();
        state.addProperty("alert", alertId);
        state.addProperty("acknowledged", true);
        return LinkActionResult.ok(state);
    }
}
