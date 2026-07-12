package za.co.neroland.neroagriculture.platform;

import java.util.ServiceLoader;

public final class Services {
    public static final NetworkPlatform NETWORK = ServiceLoader.load(NetworkPlatform.class).findFirst()
            .orElseThrow(() -> new IllegalStateException("Missing NeroAgriculture network platform"));

    /** Loader-specific facts (version, environment, dist, loaded mods) — used by telemetry. */
    public static final PlatformInfo PLATFORM = ServiceLoader.load(PlatformInfo.class).findFirst()
            .orElseThrow(() -> new IllegalStateException("Missing NeroAgriculture platform info"));

    private Services() { }
}
