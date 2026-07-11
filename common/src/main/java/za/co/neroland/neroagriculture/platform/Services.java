package za.co.neroland.neroagriculture.platform;

import java.util.ServiceLoader;

public final class Services {
    public static final NetworkPlatform NETWORK = ServiceLoader.load(NetworkPlatform.class).findFirst()
            .orElseThrow(() -> new IllegalStateException("Missing NeroAgriculture network platform"));
    private Services() { }
}
