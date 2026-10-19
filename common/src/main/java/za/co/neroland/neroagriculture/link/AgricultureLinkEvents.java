package za.co.neroland.neroagriculture.link;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.google.gson.JsonObject;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerolandcore.link.LinkAlert;
import za.co.neroland.nerolandcore.link.LinkAlerts;
import za.co.neroland.nerolandcore.link.LinkEvent;
import za.co.neroland.nerolandcore.link.NeroLinkRegistry;

import za.co.neroland.neroagriculture.NeroAgricultureCommon;
import za.co.neroland.neroagriculture.automation.AutomationOwner;
import za.co.neroland.neroagriculture.config.AgricultureConfig;
import za.co.neroland.neroagriculture.crop.GrowBedBlockEntity;
import za.co.neroland.neroagriculture.greenhouse.GreenhouseState;
import za.co.neroland.neroagriculture.terraforming.TerraformingStage;

/**
 * The live half of the link module: the two things worth waking a companion client for, and the one
 * thing worth interrupting a player for.
 *
 * <h2>Events</h2>
 *
 * <ul>
 *   <li><b>{@code greenhouse}</b> — a greenhouse enclosing one of <em>your</em> grow beds moved between
 *       {@code FORMED}, {@code BREACHED}, {@code UNPOWERED} and {@code UNFORMED}. This is the one farm
 *       transition a player genuinely cannot see coming and genuinely loses production to: an unnoticed
 *       breach quietly stalls every controlled-tier crop inside.</li>
 *   <li><b>{@code terraforming}</b> — one of your terraforming projects moved to a new stage, including
 *       completing. Long-running, unattended, and the exact shape of thing a phone is good at telling
 *       you about.</li>
 * </ul>
 *
 * <p>Crop growth itself is deliberately <b>not</b> an event. A grow bed auto-harvests and auto-replants,
 * so "your crop is ready" is not actionable, and firing per crop per stage would turn a farm into a
 * stream of noise no client would thank us for. Growth is standing state and lives in the {@code crops}
 * snapshot section instead.
 *
 * <h2>Alerts</h2>
 *
 * <p>One, and deliberately only one — an alert survives in Core's store until it is acknowledged, so it
 * is reserved for something a player would genuinely want to be told about while the game is closed:
 * <b>a greenhouse you farm in has stopped being sealed</b>. It is <b>rate-limited per controller</b>
 * ({@value #ALERT_COOLDOWN_MS} ms), so a greenhouse flapping between powered and unpowered on a
 * marginal power budget cannot turn a companion client into an alarm clock, and the alert id is one per
 * controller so a re-raise replaces rather than stacks.
 *
 * <p>A completed terraforming project raises no alert: good news does not deserve to survive in a
 * notification store until somebody dismisses it.
 *
 * <h2>Scope and privacy (POPIA/GDPR)</h2>
 *
 * <p><b>Every event here is owner-scoped; there are no broadcasts.</b> A farm is not a public place and
 * a greenhouse breach is nobody else's business, so there is no payload here that would be both safe to
 * send to every session and worth sending.
 *
 * <p>A greenhouse has no owner of its own, so its event is routed to the owners of the grow beds inside
 * it — the same indirect scoping the {@code greenhouse} snapshot section uses. Each such owner is told
 * about the building their machine is standing in; none of them is told anything about any of the
 * others, not even how many there are.
 *
 * <p>A terraforming controller with no owner (owner tracking opted out, or post-erasure) publishes
 * nothing and raises nothing: there is nobody to tell, and inventing somebody would be the exact
 * opposite of what an erasure request asked for.
 *
 * <p><b>Nothing here may throw at its caller.</b> Every publisher is wrapped: a link failure must never
 * disturb a greenhouse revalidation or a terraforming tick.
 *
 * <p>Server thread only.
 */
public final class AgricultureLinkEvents {

    /** Minimum gap between two alerts for the same greenhouse controller, in milliseconds. */
    private static final long ALERT_COOLDOWN_MS = 300_000L;

    /** {@code dimension@controller → when it was last raised}. Session state; cleared on server stop. */
    private static final Map<String, Long> LAST_ALERT = new ConcurrentHashMap<>();

    private AgricultureLinkEvents() {
    }

    /**
     * Nothing to subscribe to. NeroAgriculture publishes from the state transitions themselves — the
     * greenhouse controller's own {@code setStructuralState} and the terraforming controller's stage
     * advance are already the one place each transition is detected, and each publishes once. The method
     * exists so {@link AgricultureLinkModule#init()} has the same three-surface shape as every other
     * Nero mod's, and so a future in-mod listener has an obvious home.
     */
    static void init() {
        // Intentionally empty — see the javadoc.
    }

    /** Drops the rate-limiter state. Called from the server-stopped hook. */
    public static void reset() {
        LAST_ALERT.clear();
    }

    // --- greenhouse ------------------------------------------------------------

    /**
     * Publishes a greenhouse seal-state transition to the owners of the grow beds inside it, and raises
     * the alert when the greenhouse has just stopped being sealed.
     *
     * @param interior the controller's interior positions <em>as they were at the transition</em>; the
     *                 caller must pass this before clearing it, or there is no way left to tell whose
     *                 beds were inside
     * @param previous the state the greenhouse has just left
     * @param next     the state it has just entered
     */
    public static void greenhouseStateChanged(ServerLevel level, BlockPos controller, Set<Long> interior,
            GreenhouseState previous, GreenhouseState next, int volume, int activeCrops, int oxygen,
            @Nullable BlockPos leak) {
        if (!enabled()) {
            return;
        }
        try {
            Set<UUID> owners = ownersInside(level, interior);
            if (owners.isEmpty()) {
                return; // nobody with a stake in this building — nothing to say and nobody to say it to
            }
            String key = level.dimension().identifier() + "@" + controller.asLong();
            JsonObject payload = AgricultureLinkAccess.envelope();
            payload.addProperty("greenhouse", key);
            payload.addProperty("dimension", level.dimension().identifier().toString());
            payload.add("position", AgricultureLinkAccess.position(controller));
            payload.addProperty("state", lower(next.name()));
            payload.addProperty("previous_state", lower(previous.name()));
            payload.addProperty("sealed", next.maintainsInterior());
            payload.addProperty("volume", volume);
            payload.addProperty("active_crops", activeCrops);
            payload.addProperty("oxygen", oxygen);
            payload.addProperty("breached", leak != null);
            if (leak != null) {
                payload.add("leak", AgricultureLinkAccess.position(leak));
            }
            for (UUID owner : owners) {
                publish(LinkEvent.forPlayer(AgricultureLinkModule.MODULE_ID,
                        AgricultureLinkModule.TOPIC_GREENHOUSE, owner, payload));
            }
            if (previous.maintainsInterior() && !next.maintainsInterior()) {
                raise(level.getServer(), key, owners, LinkAlert.Severity.WARN,
                        next == GreenhouseState.UNPOWERED
                                ? "A greenhouse you farm in has run out of power and is no longer sealed."
                                : "A greenhouse you farm in is no longer sealed.");
            }
        } catch (RuntimeException e) {
            warn(AgricultureLinkModule.TOPIC_GREENHOUSE, e);
        }
    }

    /**
     * The distinct owners of the loaded grow beds whose crop position sits in {@code interior}. A grow
     * bed block is solid and therefore never in the interior itself; the space above it — where the crop
     * grows — is, which is exactly the relationship that makes the bed "inside" the greenhouse.
     */
    private static Set<UUID> ownersInside(ServerLevel level, Set<Long> interior) {
        Set<UUID> owners = new LinkedHashSet<>();
        if (interior == null || interior.isEmpty()) {
            return owners;
        }
        for (AutomationOwner.Owned owned : AutomationOwner.allTracked()) {
            if (!(owned instanceof GrowBedBlockEntity bed) || bed.isRemoved() || bed.getLevel() != level) {
                continue;
            }
            UUID owner = bed.automationOwner();
            if (owner != null && interior.contains(bed.getBlockPos().above().asLong())) {
                owners.add(owner);
            }
        }
        return owners;
    }

    // --- terraforming ----------------------------------------------------------

    /**
     * Publishes a terraforming stage transition to the project's owner. No alert: a project finishing is
     * good news, and good news does not survive in an alert store until somebody dismisses it.
     */
    public static void terraformingStageChanged(ServerLevel level, BlockPos controller,
            @Nullable UUID owner, TerraformingStage previous, TerraformingStage next, int progress) {
        if (!enabled() || owner == null) {
            return;
        }
        try {
            int total = AgricultureConfig.TERRAFORM_TOTAL_PROGRESS.get();
            JsonObject payload = AgricultureLinkAccess.envelope();
            payload.addProperty("project",
                    level.dimension().identifier() + "@" + controller.asLong());
            payload.addProperty("dimension", level.dimension().identifier().toString());
            payload.add("position", AgricultureLinkAccess.position(controller));
            payload.addProperty("stage", lower(next.name()));
            payload.addProperty("previous_stage", lower(previous.name()));
            payload.addProperty("complete", next.complete());
            payload.addProperty("progress", progress);
            payload.addProperty("progress_max", total);
            payload.addProperty("progress_percent",
                    total <= 0 ? 100 : (int) Math.max(0L, Math.min(100L, 100L * progress / total)));
            payload.addProperty("radius", AgricultureConfig.TERRAFORM_RADIUS.get());
            publish(LinkEvent.forPlayer(AgricultureLinkModule.MODULE_ID,
                    AgricultureLinkModule.TOPIC_TERRAFORMING, owner, payload));
        } catch (RuntimeException e) {
            warn(AgricultureLinkModule.TOPIC_TERRAFORMING, e);
        }
    }

    // --- plumbing --------------------------------------------------------------

    private static boolean enabled() {
        try {
            return AgricultureConfig.LINK_MODULE_ENABLED.get();
        } catch (RuntimeException e) {
            return false;
        }
    }

    /**
     * Raises one alert per listed owner through Core's per-player store, at most once per
     * {@value #ALERT_COOLDOWN_MS} ms per greenhouse. Never logs who it was raised for.
     */
    private static void raise(@Nullable MinecraftServer server, String key, Set<UUID> owners,
            LinkAlert.Severity severity, String text) {
        if (server == null || owners.isEmpty()) {
            return;
        }
        long now = System.currentTimeMillis();
        Long last = LAST_ALERT.get(key);
        if (last != null && now - last < ALERT_COOLDOWN_MS) {
            return;
        }
        LAST_ALERT.put(key, now);
        try {
            LinkAlerts alerts = LinkAlerts.get(server);
            for (UUID owner : owners) {
                alerts.raise(server, owner, LinkAlert.raise("greenhouse." + key,
                        AgricultureLinkModule.MODULE_ID, severity, text));
            }
        } catch (RuntimeException e) {
            warn("alerts", e);
        }
    }

    /** Publish to Core's shared bus; a failure there is logged, never thrown at the caller. */
    private static void publish(LinkEvent event) {
        try {
            NeroLinkRegistry.eventBus().publish(event);
        } catch (RuntimeException e) {
            warn(event.topic(), e);
        }
    }

    /** Topic only — never who the event was for (POPIA/GDPR). */
    private static void warn(String topic, RuntimeException e) {
        NeroAgricultureCommon.LOGGER.warn(
                "[NeroAgriculture] Publishing the NeroLink '{}' event failed.", topic, e);
    }

    private static String lower(String name) {
        return name.toLowerCase(java.util.Locale.ROOT);
    }
}
