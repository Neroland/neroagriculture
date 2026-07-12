package za.co.neroland.neroagriculture.terraforming;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;

import za.co.neroland.neroagriculture.environment.EnvironmentApi;
import za.co.neroland.neroagriculture.environment.EnvironmentProfile;

/**
 * Server-thread registry of completed terraformed regions. It backs an {@link EnvironmentApi} provider so a
 * finished project makes its bounded area habitable in Agriculture's environment model — WITHOUT mutating a
 * single world block or a nonexistent planetary atmosphere. Rollback simply removes the override, restoring
 * the dimension's baseline, so nothing can ever be corrupted.
 */
public final class TerraformingRegions {
    private record Region(long center, int radius) { }

    private static final Map<Identifier, Set<Region>> COMPLETED = new ConcurrentHashMap<>();

    private TerraformingRegions() { }

    /** Register the local environment override once at startup. */
    public static void register() {
        EnvironmentApi.PROVIDERS.add((level, pos) ->
                terraformed(level, pos) ? Optional.of(EnvironmentProfile.HABITABLE) : Optional.empty());
    }

    /** Pure square containment test for a region centred on (cx,cz) with the given radius. */
    public static boolean contains(int cx, int cz, int radius, int x, int z) {
        return Math.abs(x - cx) <= radius && Math.abs(z - cz) <= radius;
    }

    public static void complete(ServerLevel level, BlockPos center, int radius) {
        COMPLETED.computeIfAbsent(level.dimension().identifier(), key -> ConcurrentHashMap.newKeySet())
                .removeIf(region -> region.center() == center.asLong());
        COMPLETED.computeIfAbsent(level.dimension().identifier(), key -> ConcurrentHashMap.newKeySet())
                .add(new Region(center.asLong(), Math.max(0, radius)));
    }

    public static void rollback(ServerLevel level, BlockPos center) {
        Set<Region> regions = COMPLETED.get(level.dimension().identifier());
        if (regions != null) regions.removeIf(region -> region.center() == center.asLong());
    }

    /** True when the position lies inside any completed terraformed region in its dimension. */
    public static boolean terraformed(ServerLevel level, BlockPos pos) {
        Set<Region> regions = COMPLETED.get(level.dimension().identifier());
        if (regions == null) return false;
        for (Region region : regions) {
            BlockPos centre = BlockPos.of(region.center());
            if (contains(centre.getX(), centre.getZ(), region.radius(), pos.getX(), pos.getZ())) return true;
        }
        return false;
    }
}
