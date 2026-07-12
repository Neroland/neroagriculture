package za.co.neroland.neroagriculture.cycle;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import net.minecraft.resources.Identifier;

/**
 * Public seam for external cycle sources — Nerospace space weather, NeroEvents, etc. When no provider is
 * registered the effective external contribution is exactly 1.0 (identity), so cycles work fully standalone.
 */
public final class CycleApi {
    @FunctionalInterface
    public interface Provider {
        CycleModifier modifier(Identifier dimension, long time);
    }

    public static final List<Provider> PROVIDERS = new CopyOnWriteArrayList<>();

    private CycleApi() { }
}
