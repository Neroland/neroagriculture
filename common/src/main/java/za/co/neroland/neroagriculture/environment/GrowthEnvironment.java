package za.co.neroland.neroagriculture.environment;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/** Resolves the effective open-air environment at a position: Nerospace provider when present, else local. */
public final class GrowthEnvironment {
    private GrowthEnvironment() { }

    public static EnvironmentProfile worldProfile(ServerLevel level, BlockPos pos) {
        for (EnvironmentApi.WorldEnvironmentProvider provider : EnvironmentApi.PROVIDERS) {
            var supplied = provider.at(level, pos);
            if (supplied.isPresent()) return supplied.get();
        }
        return DimensionEnvironments.profileFor(level.getServer(), level.dimension().identifier());
    }
}
