package za.co.neroland.neroagriculture.compat.nerospace;

import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

import za.co.neroland.neroagriculture.NeroAgricultureCommon;
import za.co.neroland.neroagriculture.compat.CompatContracts;
import za.co.neroland.neroagriculture.config.AgricultureConfig;
import za.co.neroland.neroagriculture.environment.EnvironmentApi;
import za.co.neroland.neroagriculture.environment.EnvironmentProfile;
import za.co.neroland.neroagriculture.environment.OxygenApi;
import za.co.neroland.neroagriculture.environment.Temperature;
import za.co.neroland.neroagriculture.lifecycle.ServerStateReset;
import za.co.neroland.neroagriculture.platform.Services;

/**
 * Optional two-way bridge to Nerospace's public {@code za.co.neroland.nerospace.api} surface: Nerospace
 * answers what the atmosphere is like on its planets, and NeroAgriculture greenhouses feed their oxygen
 * production back into it.
 *
 * <h2>Mechanism (decision record)</h2>
 * <ul>
 *   <li><b>Compile-only, not reflection.</b> Nerospace publishes its six loader cells to GitHub Packages,
 *       so its API is a real {@code compileOnly} artifact here — see {@code nerospace_version} in
 *       {@code gradle.properties}. {@link NerospaceVisitBridge}'s join-time visit history uses the same
 *       artifact through its own holder class. Nothing in this mod reflects.</li>
 *   <li><b>Absent-safe by class loading, not by {@code Class.forName}.</b> Every reference to a Nerospace
 *       type lives inside the nested {@link Api} holder. The JVM loads a nested class on first use, so
 *       when Nerospace is not installed {@link Api} is never touched and never fails to link. The
 *       presence check itself reads the loaded-mod list through {@link Services#PLATFORM}, exactly as
 *       {@code SiblingOverlays} does.</li>
 *   <li><b>Lazy presence check.</b> Resolved on first query rather than at mod init, because the loaded-mod
 *       list is not reliably complete during construction on every loader. It is cached after the first
 *       resolution, so the steady-state cost is one volatile read.</li>
 *   <li><b>Only Nerospace dimensions are answered.</b> The provider returns empty for the overworld, the
 *       nether, the end and every other mod's dimension, so Agriculture's own {@code DimensionEnvironments}
 *       datapack layer keeps full authority everywhere Nerospace has no opinion. A provider that answered
 *       for the overworld would silently override server datapacks.</li>
 * </ul>
 *
 * <h2>What is deliberately not wired</h2>
 * <ul>
 *   <li><b>{@code CycleApi}</b> — Nerospace exposes no public space-weather or stellar-cycle query, so
 *       there is nothing to source a {@code CycleModifier} from. Registering a provider that always
 *       returned identity would be worse than registering none.</li>
 *   <li><b>{@code AgricultureApi.TerraformingListener} → Nerospace overlays</b> — Nerospace's overlay API
 *       is claim-gated and denies every mutation until the server installs a {@code TerraformClaimPolicy},
 *       and it needs an acting player, which a block-entity terraforming tick does not have. Forwarding
 *       our events would therefore be a silent no-op on every server. Agriculture's terraforming stays in
 *       its own reversible model until a server-side claim policy exists to authorise the hand-off.</li>
 * </ul>
 *
 * <h2>Privacy (POPIA/GDPR)</h2>
 * Nothing player-keyed crosses this seam in either direction. The oxygen source id is derived from the
 * greenhouse controller's block position — Nerospace persists that id verbatim and its API documents that
 * source ids must never encode player identity, which this one does not. The environment query is a pure
 * read and carries no actor at all.
 *
 * <p>Disable with {@code compat.nerospace_environment = false} (default {@code true}).</p>
 */
public final class NerospaceEnvironmentBridge {

    private static final String NEROSPACE_MOD_ID = "nerospace";

    /**
     * Radius, in blocks, that a greenhouse's oxygen contribution reaches into Nerospace's atmosphere.
     * Agriculture's seam publishes an amount and a controller position but no radius, so the bridge picks
     * one: large enough to cover a greenhouse and its doorstep, far short of pressurising a base.
     */
    private static final int CONTRIBUTION_RADIUS = 16;

    /**
     * How many 32-block volume units' worth of capped output counts as "full strength" on Nerospace's
     * 0-15 oxygen scale — i.e. a roughly 256-block greenhouse running at its per-volume cap.
     *
     * <p>This is the balance knob for the whole seam. Agriculture's amount is an absolute figure that
     * grows with interior volume ({@code greenhouse.oxygen_cap_per_32_volume} × volume units) while
     * Nerospace's strength is a fixed 0-15 field scale, so the two need a ratio rather than a clamp — a
     * clamp would saturate at 15 for essentially every real greenhouse and make a modest one enough to
     * turn a sphere of vacuum permanently breathable.</p>
     */
    private static final int FULL_STRENGTH_VOLUME_UNITS = 8;

    /**
     * Nerospace's oxygen field scale maximum. Deliberately a local literal rather than a read of
     * {@code NerospaceOxygen.MAX_STRENGTH}: naming a Nerospace type outside the {@link Api} holder is
     * exactly what would break the absent-mod guarantee, and relying on javac inlining a foreign
     * compile-time constant would make that guarantee depend on the other mod never changing the field
     * to a non-constant. {@code Api.contributeOxygen} re-clamps against the real constant anyway.
     */
    private static final int MAX_FIELD_STRENGTH = 15;

    /**
     * Lifetime of a published contribution, as a multiple of the greenhouse revalidation interval.
     * Derived rather than fixed because {@code greenhouse.revalidate_ticks} is configurable up to 12,000
     * — a constant tuned for the 100-tick default would silently expire between refreshes on a server
     * that raised it.
     */
    private static final int CONTRIBUTION_REFRESH_MULTIPLE = 3;

    /** Floor for the derived lifetime, so a very short revalidation interval still gives sane air. */
    private static final long MIN_CONTRIBUTION_TICKS = 20L * 60L;

    /** Null until the first query resolves it; then a cached yes/no for the rest of the session. */
    private static volatile Boolean nerospacePresent;

    private static EnvironmentApi.WorldEnvironmentProvider environmentProvider;
    private static OxygenApi.Consumer oxygenConsumer;

    private NerospaceEnvironmentBridge() { }

    /** Registers both seams. Called once from common init; a no-op when the config toggle is off. */
    public static void register() {
        if (!AgricultureConfig.NEROSPACE_ENVIRONMENT.get()) {
            return;
        }
        environmentProvider = NerospaceEnvironmentBridge::environmentAt;
        oxygenConsumer = NerospaceEnvironmentBridge::publishOxygen;
        CompatContracts.registerEnvironmentProvider(environmentProvider);
        CompatContracts.registerOxygenConsumer(oxygenConsumer);
    }

    /**
     * Clears server-scoped state on server stop.
     *
     * <p>It deliberately does <b>not</b> unregister the two seams. {@link #register()} runs once per JVM
     * from mod construction, so unregistering here would silently disable the bridge for every world
     * loaded afterwards in the same session — leave a single-player world, load another, and Nerospace
     * would stop answering with nothing in the log to say why. Both lambdas are stateless and hold no
     * reference to the server, so leaving them registered pins nothing.</p>
     */
    public static void reset() {
        nerospacePresent = null;
    }

    /** Unregisters both seams. Not part of the normal lifecycle; kept for tests and for hot reload. */
    public static void unregister() {
        if (environmentProvider != null) {
            CompatContracts.removeEnvironmentProvider(environmentProvider);
            environmentProvider = null;
        }
        if (oxygenConsumer != null) {
            CompatContracts.removeOxygenConsumer(oxygenConsumer);
            oxygenConsumer = null;
        }
        nerospacePresent = null;
    }

    // --- Environment ---------------------------------------------------------

    private static Optional<EnvironmentProfile> environmentAt(ServerLevel level, BlockPos pos) {
        if (level == null || pos == null || !available()) {
            return Optional.empty();
        }
        return Api.profileAt(level, pos);
    }

    // --- Oxygen --------------------------------------------------------------

    private static void publishOxygen(OxygenApi.Contribution contribution) {
        if (contribution == null || !available()) {
            return;
        }
        MinecraftServer server = ServerStateReset.currentServer();
        if (server == null) {
            return;
        }
        ServerLevel level = server.getLevel(ResourceKey.create(Registries.DIMENSION, contribution.dimension()));
        if (level == null) {
            return;
        }
        // Position-derived, never player-derived. Unique per controller, and per level because the
        // Nerospace store is itself per-dimension.
        Identifier source = Identifier.fromNamespaceAndPath(NeroAgricultureCommon.MOD_ID,
                "greenhouse/" + Long.toHexString(contribution.controllerPos()));
        if (contribution.amount() <= 0) {
            // Amount 0 is the seam's explicit retraction (breach, unpowered, unformed, removed).
            Api.removeOxygen(level, source);
            return;
        }
        Api.contributeOxygen(level, source, BlockPos.of(contribution.controllerPos()),
                CONTRIBUTION_RADIUS, strengthFor(contribution.amount()), contributionTicks());
    }

    /**
     * Maps Agriculture's absolute, volume-scaled oxygen amount onto Nerospace's 0-15 field scale.
     * Proportional, so a small greenhouse contributes proportionally little; only one at or above
     * {@link #FULL_STRENGTH_VOLUME_UNITS} units of capped output reads as full strength.
     */
    private static int strengthFor(int amount) {
        int perUnitCap = Math.max(1, AgricultureConfig.GREENHOUSE_OXYGEN_CAP_PER_32.get());
        int fullStrengthAmount = perUnitCap * FULL_STRENGTH_VOLUME_UNITS;
        // At least 1: a greenhouse that produces anything at all should register as something.
        return Math.max(1, Math.round((float) amount * MAX_FIELD_STRENGTH / fullStrengthAmount));
    }

    private static long contributionTicks() {
        long derived = (long) AgricultureConfig.GREENHOUSE_REVALIDATE_TICKS.get()
                * CONTRIBUTION_REFRESH_MULTIPLE;
        return Math.max(MIN_CONTRIBUTION_TICKS, derived);
    }

    // --- Presence ------------------------------------------------------------

    /**
     * Whether Nerospace is loaded, for the other Nerospace compat class in this package. Same cached
     * answer as this bridge's own check; independent of the {@code compat.nerospace_environment} toggle.
     */
    static boolean nerospaceLoaded() {
        return available();
    }

    private static boolean available() {
        Boolean cached = nerospacePresent;
        if (cached != null) {
            return cached;
        }
        boolean present = isModLoaded(NEROSPACE_MOD_ID);
        nerospacePresent = present;
        NeroAgricultureCommon.LOGGER.info(
                "[NeroAgriculture] Nerospace {}.",
                present ? "detected — Nerospace compat active"
                        : "not installed — using the local environment model");
        return present;
    }

    /** Mirrors {@code SiblingOverlays}: public manifest ids only, never anything player-identifying. */
    private static boolean isModLoaded(String modId) {
        try {
            for (String entry : Services.PLATFORM.getLoadedModIds()) {
                int space = entry.indexOf(' ');
                if (modId.equals(space < 0 ? entry : entry.substring(0, space))) {
                    return true;
                }
            }
        } catch (RuntimeException e) {
            // A loader that cannot answer yet is treated as "absent" rather than allowed to break a tick.
            NeroAgricultureCommon.LOGGER.debug("[NeroAgriculture] Could not read the loaded-mod list.", e);
        }
        return false;
    }

    /**
     * The only place in this mod that names a Nerospace type. Loaded by the JVM on first use, which is
     * guarded by {@link #available()} — so with Nerospace absent this class is never linked.
     *
     * <p>Every method here keeps Nerospace types out of its own signature, so verifying the calling
     * lambdas never drags this class in either.</p>
     */
    private static final class Api {

        private Api() { }

        static Optional<EnvironmentProfile> profileAt(ServerLevel level, BlockPos pos) {
            za.co.neroland.nerospace.api.EnvironmentSnapshot snapshot =
                    za.co.neroland.nerospace.api.NerospaceEnvironment.at(level, pos);

            // Not a Nerospace dimension: stay silent so the local datapack model keeps authority.
            if (snapshot.planet().isEmpty()) {
                return Optional.empty();
            }
            // Unloaded means "no reading", not "vacuum". Defer rather than assert something false.
            if (!snapshot.loaded()) {
                return Optional.empty();
            }

            // Terraforming is what makes a dead world liveable — from stage 1 Nerospace has rewritten the
            // biome and flagged the chunk breathable, so the planet's ambient hazard no longer applies.
            // The default is not dead code: Hazard is a foreign enum whose owner reserves the right to
            // append constants, and an exhaustive switch would throw on a crop tick the day it does.
            Temperature temperature = snapshot.terraformStage() >= 1
                    ? Temperature.TEMPERATE
                    : switch (snapshot.hazard()) {
                        case HEAT -> Temperature.HOT;
                        case COLD -> Temperature.COLD;
                        default -> Temperature.TEMPERATE;
                    };

            boolean pressurised =
                    snapshot.atmosphere() != za.co.neroland.nerospace.api.Atmosphere.VACUUM;
            return Optional.of(new EnvironmentProfile(temperature, snapshot.breathable(), pressurised));
        }

        /** {@code strength} has already been mapped onto the field scale; this only re-clamps it. */
        static void contributeOxygen(ServerLevel level, Identifier source, BlockPos center, int radius,
                int strength, long durationTicks) {
            int bounded = Math.clamp(strength, 1,
                    za.co.neroland.nerospace.api.NerospaceOxygen.MAX_STRENGTH);
            long boundedDuration = Math.clamp(durationTicks, 1L,
                    za.co.neroland.nerospace.api.NerospaceOxygen.MAX_DURATION_TICKS);
            za.co.neroland.nerospace.api.NerospaceOxygen.contribute(level, source, center, radius,
                    bounded, boundedDuration);
        }

        static void removeOxygen(ServerLevel level, Identifier source) {
            za.co.neroland.nerospace.api.NerospaceOxygen.remove(level, source);
        }
    }
}
