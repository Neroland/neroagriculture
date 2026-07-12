package za.co.neroland.neroagriculture.environment;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/**
 * Public seam for supplying world environment from another mod. Nerospace (or any provider) may register a
 * {@link WorldEnvironmentProvider}; when none is present the local {@link DimensionEnvironments} model is used.
 */
public final class EnvironmentApi {
    @FunctionalInterface
    public interface WorldEnvironmentProvider {
        Optional<EnvironmentProfile> at(ServerLevel level, BlockPos pos);
    }

    public static final List<WorldEnvironmentProvider> PROVIDERS = new CopyOnWriteArrayList<>();

    private EnvironmentApi() { }
}
