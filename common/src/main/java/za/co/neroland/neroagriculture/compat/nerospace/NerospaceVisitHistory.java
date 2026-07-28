package za.co.neroland.neroagriculture.compat.nerospace;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.neroagriculture.NeroAgricultureCommon;
import za.co.neroland.neroagriculture.platform.Services;

/**
 * Reflective consumer of Nerospace's historical planet-visit query. Nerospace's only supported
 * integration surface is the semver-stable {@code za.co.neroland.nerospace.api} facade; the
 * {@code 1.0.0-beta.8} API floor (BETA-0.1.0-PLAN Stage 1) adds historical visits to it as
 * {@code NerospaceVisits}. Because Nerospace is deliberately absent from the compile classpath, this
 * class binds to that facade purely by name at runtime:
 *
 * <ol>
 *   <li><b>Presence:</b> {@code nerospace} must appear in the platform's loaded-mod list (the same
 *       {@code PlatformInfo.getLoadedModIds()} seam SiblingOverlays uses).</li>
 *   <li><b>Version floor:</b> the reported version must not be provably below
 *       {@code 1.0.0-beta.8} ({@link #meetsFloor}); unparseable versions fall through to the class
 *       probe, which is authoritative anyway.</li>
 *   <li><b>Probe (once):</b> resolve {@code za.co.neroland.nerospace.api.NerospaceVisits} and its
 *       public static per-player history query {@code (MinecraftServer, UUID) -> Collection} — tried
 *       under the facade's documented candidate names ({@code visitedDimensions}, {@code
 *       visitedPlanets}, {@code visited}, {@code history}). Elements are mapped to dimension ids via
 *       {@code Identifier} / {@code ResourceKey} directly or a {@code PlanetId}-style {@code id()} /
 *       {@code dimension()} accessor.</li>
 * </ol>
 *
 * Every failure path — mod absent, floor unmet, class or method missing, invocation error — collapses
 * to an empty result and permanently disables further attempts, so the bridge degrades to live-visit
 * tracking only. One anonymous info line records the outcome; no player data is ever logged.
 */
final class NerospaceVisitHistory {

    private static final String MOD_ID = "nerospace";
    private static final String API_CLASS = "za.co.neroland.nerospace.api.NerospaceVisits";
    private static final String[] QUERY_NAMES = {"visitedDimensions", "visitedPlanets", "visited", "history"};
    private static final int BETA_FLOOR = 8;
    private static final Pattern SEMVER = Pattern.compile("^(\\d+)\\.(\\d+)\\.(\\d+)(?:-([a-z]+)\\.?(\\d+)?)?$");

    private enum State { UNPROBED, AVAILABLE, UNAVAILABLE }

    private static volatile State state = State.UNPROBED;
    @Nullable private static volatile Method query;

    private NerospaceVisitHistory() { }

    /**
     * The dimension ids of every Nerospace planet this player has historically visited, or an empty
     * set when the history surface is unavailable for any reason. Never throws.
     */
    static Set<Identifier> visitedDimensions(MinecraftServer server, UUID player) {
        if (state == State.UNPROBED) probe();
        Method resolved = query;
        if (state != State.AVAILABLE || resolved == null) return Set.of();
        try {
            Object result = resolved.invoke(null, server, player);
            if (!(result instanceof Collection<?> visits)) return Set.of();
            Set<Identifier> dimensions = new HashSet<>();
            for (Object visit : visits) {
                Identifier dimension = toDimensionId(visit);
                if (dimension != null) dimensions.add(dimension);
            }
            return dimensions;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            state = State.UNAVAILABLE;
            query = null;
            NeroAgricultureCommon.LOGGER.info(
                    "[NeroAgriculture] Nerospace visit history disabled after query error: {}", e.toString());
            return Set.of();
        }
    }

    private static synchronized void probe() {
        if (state != State.UNPROBED) return;
        state = State.UNAVAILABLE;
        String version = installedVersion();
        if (version == null) return; // Nerospace absent — live tracking has nothing to backfill anyway
        if (!meetsFloor(version)) {
            NeroAgricultureCommon.LOGGER.info(
                    "[NeroAgriculture] Nerospace {} is below the 1.0.0-beta.8 visit-history API floor; "
                    + "planet visits are tracked live only.", version);
            return;
        }
        try {
            Class<?> api = Class.forName(API_CLASS);
            for (String name : QUERY_NAMES) {
                for (Method method : api.getMethods()) {
                    if (!method.getName().equals(name) || !Modifier.isStatic(method.getModifiers())) continue;
                    Class<?>[] parameters = method.getParameterTypes();
                    if (parameters.length == 2 && parameters[0] == MinecraftServer.class
                            && parameters[1] == UUID.class
                            && Collection.class.isAssignableFrom(method.getReturnType())) {
                        query = method;
                        state = State.AVAILABLE;
                        NeroAgricultureCommon.LOGGER.info(
                                "[NeroAgriculture] Nerospace visit history connected via {}#{}.",
                                API_CLASS, name);
                        return;
                    }
                }
            }
            NeroAgricultureCommon.LOGGER.info(
                    "[NeroAgriculture] Nerospace {} exposes no recognised visit-history query; "
                    + "planet visits are tracked live only.", version);
        } catch (ClassNotFoundException | LinkageError e) {
            NeroAgricultureCommon.LOGGER.info(
                    "[NeroAgriculture] Nerospace {} has no visit-history API; planet visits are tracked "
                    + "live only.", version);
        }
    }

    /** Maps one returned visit element to a dimension id, or null when no shape matches. */
    @Nullable
    private static Identifier toDimensionId(@Nullable Object visit) {
        if (visit == null) return null;
        if (visit instanceof Identifier identifier) return identifier;
        if (visit instanceof ResourceKey<?> key) return key.identifier();
        Object mapped = invokeAccessor(visit, "id");
        if (mapped == null) mapped = invokeAccessor(visit, "dimension");
        if (mapped instanceof Identifier identifier) return identifier;
        if (mapped instanceof ResourceKey<?> key) return key.identifier();
        return null;
    }

    @Nullable
    private static Object invokeAccessor(Object target, String name) {
        try {
            return target.getClass().getMethod(name).invoke(target);
        } catch (ReflectiveOperationException | RuntimeException e) {
            return null;
        }
    }

    /** Nerospace's reported version ("" when listed without one), or null when it is not loaded. */
    @Nullable
    private static String installedVersion() {
        for (String entry : Services.PLATFORM.getLoadedModIds()) {
            if (entry.equals(MOD_ID)) return "";
            if (entry.startsWith(MOD_ID + " ")) return entry.substring(MOD_ID.length() + 1).trim();
        }
        return null;
    }

    /**
     * False only when {@code version} is provably below {@code 1.0.0-beta.8}; blank or unrecognised
     * strings return true so the (authoritative) class probe decides. Package-private for tests.
     */
    static boolean meetsFloor(String version) {
        String v = version.trim().toLowerCase(Locale.ROOT);
        int metadata = v.indexOf('+');
        if (metadata >= 0) v = v.substring(0, metadata);
        if (v.isEmpty()) return true;
        Matcher m = SEMVER.matcher(v);
        if (!m.matches()) return true;
        int major = Integer.parseInt(m.group(1));
        int minor = Integer.parseInt(m.group(2));
        int patch = Integer.parseInt(m.group(3));
        if (major != 1 || minor != 0 || patch != 0) {
            return major > 1 || (major == 1 && (minor > 0 || patch > 0));
        }
        String prerelease = m.group(4);
        if (prerelease == null) return true; // 1.0.0 release sorts above every 1.0.0-* prerelease
        if ("beta".equals(prerelease)) {
            String number = m.group(5);
            return number != null && Integer.parseInt(number) >= BETA_FLOOR;
        }
        return "rc".equals(prerelease); // alpha/dev prereleases sort below beta
    }
}
