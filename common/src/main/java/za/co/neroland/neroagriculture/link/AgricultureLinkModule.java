package za.co.neroland.neroagriculture.link;

import java.util.List;

import za.co.neroland.nerolandcore.link.LinkModuleInfo;
import za.co.neroland.nerolandcore.link.NeroLinkRegistry;

import za.co.neroland.neroagriculture.NeroAgricultureCommon;
import za.co.neroland.neroagriculture.config.AgricultureConfig;
import za.co.neroland.neroagriculture.platform.Services;

/**
 * NeroAgriculture's plug into Neroland Core's link API — the seam a companion client reaches a player's
 * own farm through, without NeroAgriculture knowing that any such client exists.
 *
 * <p>The whole module is plain server-side Java against Core's
 * {@link za.co.neroland.nerolandcore.link} package: no loader wiring, no networking of its own, no HTTP.
 * NeroAgriculture registers what it can show and what it can do; the separate NeroLink bridge mod reads
 * Core's registry and serves it. With no bridge installed this costs one registry entry.
 *
 * <p>Three surfaces, all registered from {@link NeroAgricultureCommon#init()}:
 *
 * <ul>
 *   <li><b>Read</b> — {@link AgricultureLinkSnapshots}, serving {@code crops}, {@code machines},
 *       {@code greenhouse} and {@code fragments};</li>
 *   <li><b>Write</b> — {@link AgricultureLinkActions}, accepting {@code acknowledge_alert} and nothing
 *       else;</li>
 *   <li><b>Live</b> — {@link AgricultureLinkEvents}, publishing greenhouse and terraforming transitions
 *       onto Core's shared event bus and raising the one alert this mod has any business raising.</li>
 * </ul>
 *
 * <h2>Ownership is the whole visibility rule</h2>
 *
 * <p>NeroAgriculture's only notion of "whose is this" is
 * {@link za.co.neroland.neroagriculture.automation.AutomationOwner}: a bare owner UUID recorded in the
 * NBT of four block entities — grow beds, the area machines (Planter / Harvester / Fertiliser
 * Applicator), the fabrication machines, and the Terraforming Controller. Every section below starts
 * from {@code AutomationOwner.ownedBy(playerId)} and reports nothing that is not reachable from a block
 * the requesting player owns.
 *
 * <h2>Why there is no {@code towers} section</h2>
 *
 * <p>The Crop Tower Controller <b>has no owner field at all</b>. Its gate checks resolve a player with
 * {@code level.getNearestPlayer(..., 24.0)}, which is a reasonable-enough approximation for "who is
 * standing here operating this" and a completely unacceptable one for "who may read this over the
 * internet" — proximity is not permission, and a player idling near somebody else's tower would be
 * handed its contents. Nor is there a containing structure to inherit ownership from: a tower is a
 * controller plus a frame column and belongs to nothing.
 *
 * <p>So the section is omitted rather than approximated. Towers become reportable the day the
 * controller records a placing player like the other four block entities do (which is the open audit
 * item behind the {@code getNearestPlayer} gate check as well); at that point {@code towers} is an
 * additive section and a {@link #SCHEMA_VERSION} bump.
 *
 * <p>The {@code greenhouse} section survives the same test only because it is scoped <em>indirectly</em>
 * — see {@link AgricultureLinkSnapshots}. A greenhouse controller has no owner either, so a greenhouse
 * is reported to a player only when it encloses a grow bed that player owns. The permission comes from
 * the bed, which really does carry a UUID; the greenhouse is just the thing the bed is standing in.
 *
 * <h2>Privacy (POPIA/GDPR)</h2>
 *
 * <p><b>Own machines only.</b> Every snapshot section is scoped to the requesting {@code playerId}
 * before anything leaves this mod, and nothing is reported about any other player — not their machines,
 * not their names, not that they exist.
 *
 * <p><b>Coordinates.</b> A machine's own position is in its row, because it is the one thing a companion
 * app needs to tell two of your farms apart and it is the requesting player's own block. Nothing else
 * carries a position: the greenhouse section reports the enclosing greenhouse's controller position (a
 * building the player has a bed inside) and the fragment ladder reports no position at all.
 *
 * <p><b>No broadcast topics.</b> Unlike a colony, a farm is not a place other players have any business
 * being notified about, so every event this module publishes is owner-scoped. There is nothing here that
 * would be safe to broadcast and useful at the same time.
 *
 * <p><b>Erasure needs no separate wiring.</b> Every read starts from the live owner UUID on a block
 * entity, so a player erased through Core's {@code PlayerDataErasure} hook immediately owns nothing and
 * every section answers empty for them. See {@code PRIVACY.md} and {@code wiki/Link-Module.md}.
 *
 * <p><b>Schema version 1.</b> Bump {@link #SCHEMA_VERSION} whenever the shape of a snapshot section
 * changes, so a companion client can tell what it is parsing.
 */
public final class AgricultureLinkModule {

    /** The link module id — the same string as the mod id, as the ecosystem convention requires. */
    public static final String MODULE_ID = NeroAgricultureCommon.MOD_ID;

    /** The snapshot schema revision. Bump on any change to a section's shape. */
    public static final int SCHEMA_VERSION = 1;

    /** Section: the player's own grow beds — tier, resources, and the growth of the crop above each. */
    public static final String SECTION_CROPS = "crops";

    /** Section: the player's own fabrication, area and terraforming machines and what each is doing. */
    public static final String SECTION_MACHINES = "machines";

    /** Section: the greenhouses enclosing the player's own grow beds — seal state, oxygen, upkeep. */
    public static final String SECTION_GREENHOUSE = "greenhouse";

    /** Section: the player's progress up the Cosmic-ascent fragment ladder. */
    public static final String SECTION_FRAGMENTS = "fragments";

    /** Action: acknowledge one of your own NeroAgriculture alerts in Core's alert store. */
    public static final String ACTION_ACKNOWLEDGE_ALERT = "acknowledge_alert";

    /** Topic: a greenhouse enclosing one of your grow beds changed seal state. Owner-scoped. */
    public static final String TOPIC_GREENHOUSE = "greenhouse";

    /** Topic: one of your terraforming projects moved to a new stage. Owner-scoped. */
    public static final String TOPIC_TERRAFORMING = "terraforming";

    private AgricultureLinkModule() {
    }

    /**
     * Register the read, write and live surfaces with Core. Called <b>last</b> from
     * {@link NeroAgricultureCommon#init()}, so a companion client is never told about something before
     * the mod itself has finished setting it up.
     *
     * <p>A failure here must never take the mod down with it: farms work perfectly well with no link
     * module, so any problem is logged and swallowed. The same is true of the config switch —
     * {@code link.module_enabled=false} simply means nothing is registered, and every publisher checks
     * the same flag before it speaks.
     */
    public static void init() {
        try {
            if (!AgricultureConfig.LINK_MODULE_ENABLED.get()) {
                NeroAgricultureCommon.LOGGER.info(
                        "[NeroAgriculture] The NeroLink module is disabled by config; companion clients "
                                + "will not see NeroAgriculture data.");
                return;
            }
            LinkModuleInfo info = new LinkModuleInfo(MODULE_ID, modVersion(), SCHEMA_VERSION,
                    List.of(SECTION_CROPS, SECTION_MACHINES, SECTION_GREENHOUSE, SECTION_FRAGMENTS),
                    List.of(ACTION_ACKNOWLEDGE_ALERT));
            // One provider and one handler cover the whole module; Core keys both on the module id.
            NeroLinkRegistry.registerSnapshotProvider(new AgricultureLinkSnapshots(), info);
            NeroLinkRegistry.registerActionHandler(new AgricultureLinkActions(), info);
            AgricultureLinkEvents.init();
        } catch (RuntimeException e) {
            NeroAgricultureCommon.LOGGER.warn(
                    "[NeroAgriculture] Could not register the NeroLink module; companion clients will "
                            + "not see NeroAgriculture data. Farming itself is unaffected.", e);
        }
    }

    /** This mod's public version string for discovery, or {@code "unknown"} if the seam is unhappy. */
    private static String modVersion() {
        try {
            String version = Services.PLATFORM.getModVersion();
            return version == null || version.isBlank() ? "unknown" : version;
        } catch (RuntimeException e) {
            return "unknown";
        }
    }
}
