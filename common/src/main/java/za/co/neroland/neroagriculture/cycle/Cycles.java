package za.co.neroland.neroagriculture.cycle;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.neroagriculture.config.AgricultureConfig;

/**
 * Resolves the active cycle modifier for a dimension. The result is cached on coarse time buckets — never
 * evaluated per tick or per crop — and combines the datapack profile with any registered {@link CycleApi}
 * providers (Nerospace/NeroEvents). With cycles disabled or absent, everything resolves to identity (1.0).
 */
public final class Cycles {
    private static final long COARSE_TICKS = 200L;

    private record Cached(long bucket, CycleModifier modifier) { }

    private static final Map<Identifier, Cached> CACHE = new ConcurrentHashMap<>();

    private Cycles() { }

    /** Drop every cached bucket when the server stops so stale modifiers never leak into the next world (thread-safe). */
    public static void clearCache() {
        CACHE.clear();
    }

    public static CycleModifier current(@Nullable MinecraftServer server, Identifier dimension, long time) {
        if (!AgricultureConfig.CYCLES_ENABLED.get()) return CycleModifier.IDENTITY;
        long bucket = Math.floorDiv(time, COARSE_TICKS);
        Cached cached = CACHE.get(dimension);
        if (cached != null && cached.bucket == bucket) return cached.modifier;
        CycleModifier computed = compute(server, dimension, time);
        CACHE.put(dimension, new Cached(bucket, computed));
        return computed;
    }

    private static CycleModifier compute(@Nullable MinecraftServer server, Identifier dimension, long time) {
        CycleProfile profile = CycleCatalog.profile(server, dimension);
        CycleModifier base = profile == null ? CycleModifier.IDENTITY : profile.modifierAt(time);
        for (CycleApi.Provider provider : CycleApi.PROVIDERS) base = base.combine(provider.modifier(dimension, time));
        return base;
    }

    /** Short forecast for diagnostics: active phase key and seconds until the next phase. */
    public static String describe(@Nullable MinecraftServer server, Identifier dimension, long time) {
        if (!AgricultureConfig.CYCLES_ENABLED.get()) return "off";
        CycleProfile profile = CycleCatalog.profile(server, dimension);
        if (profile == null) return "none";
        CycleModifier modifier = current(server, dimension, time);
        return profile.phaseAt(time).displayKey() + " x" + modifier.growth() + "/" + modifier.yield()
                + " next=" + profile.nextPhase(time).displayKey() + " in " + profile.ticksUntilNextPhase(time) / 20 + "s";
    }
}
