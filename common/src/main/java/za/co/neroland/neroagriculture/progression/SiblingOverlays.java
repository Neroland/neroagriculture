package za.co.neroland.neroagriculture.progression;

import java.util.Locale;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.neroagriculture.config.AgricultureConfig;
import za.co.neroland.neroagriculture.content.FragmentTier;
import za.co.neroland.neroagriculture.platform.Services;
import za.co.neroland.nerolandcore.progression.CoreGates;
import za.co.neroland.nerolandcore.progression.ProgressionGates;

/**
 * Optional cross-mod progression overlays. On top of NeroAgriculture's own native tier gates, a higher
 * tier may <em>additionally</em> require the matching Neroland arc gate (industrial power, reached
 * orbit, first colony, deep space) that a sibling mod drives open — giving modpacks the cross-mod
 * "link" feel without ever blocking standalone play.
 *
 * <p>Controlled by {@code progression.sibling_overlays}:
 * <ul>
 *   <li><b>auto</b> (default) — overlay a tier only when the sibling mod that can open that arc gate is
 *       actually loaded, so a Core-only game is never gated by an arc gate nothing can open;</li>
 *   <li><b>on</b> — always overlay (for packs that drive the arc gates by other means);</li>
 *   <li><b>off</b> — never overlay; only the native gates apply.</li>
 * </ul>
 */
public final class SiblingOverlays {
    private SiblingOverlays() { }

    private enum Mode { AUTO, ON, OFF }

    /**
     * Whether the sibling overlay for {@code tier} is satisfied for {@code player}. Returns {@code true}
     * whenever the overlay does not apply (mode off, tier has no arc gate, or — in auto mode — the
     * opener mod is absent), so the native gate alone governs and standalone play is never blocked.
     */
    public static boolean tierSatisfied(ServerPlayer player, FragmentTier tier) {
        if (!requiresArcGate(tier, AgricultureConfig.SIBLING_OVERLAYS.get(), isModLoaded(openerMod(tier)))) {
            return true;
        }
        return ProgressionGates.isOpen(player, arcGate(tier));
    }

    /**
     * Pure decision (no player/server needed, so it is unit-testable): whether the sibling arc gate for
     * {@code tier} must additionally be open, given the raw config {@code mode} and whether the opener
     * mod is loaded. In auto mode this is true only when the opener mod is present — which is why a
     * Core-only game (no opener loaded) is never gated by an arc gate nothing could open.
     */
    static boolean requiresArcGate(FragmentTier tier, String mode, boolean openerLoaded) {
        Mode resolved = mode(mode);
        if (resolved == Mode.OFF || arcGate(tier) == null) return false;
        return resolved == Mode.ON || openerLoaded;
    }

    @Nullable
    private static Identifier arcGate(FragmentTier tier) {
        return switch (tier) {
            case TERRITE -> null;
            case FORGITE -> CoreGates.INDUSTRIAL_POWER;
            case ORBITE -> CoreGates.REACHED_ORBIT;
            case COLONITE -> CoreGates.FIRST_COLONY;
            case VOIDITE -> CoreGates.DEEP_SPACE;
        };
    }

    /** The sibling mod that can drive the arc gate open for {@code tier} (used only in auto mode). */
    private static String openerMod(FragmentTier tier) {
        return switch (tier) {
            case TERRITE -> "";
            case FORGITE -> "nerotech";
            case ORBITE -> "nerospace";
            case COLONITE -> "nerocolonies";
            case VOIDITE -> "nerospace";
        };
    }

    private static Mode mode(String raw) {
        return switch (raw.trim().toLowerCase(Locale.ROOT)) {
            case "on" -> Mode.ON;
            case "off" -> Mode.OFF;
            default -> Mode.AUTO;
        };
    }

    private static boolean isModLoaded(String modId) {
        if (modId.isEmpty()) return false;
        for (String entry : Services.PLATFORM.getLoadedModIds()) {
            if (entry.equals(modId) || entry.startsWith(modId + " ")) return true;
        }
        return false;
    }
}
