package za.co.neroland.neroagriculture.automation;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import org.jetbrains.annotations.Nullable;

/**
 * Claim/protection seam for automated area work. Standalone there are no guards, so machines operate freely
 * on loaded chunks. A claims/protection mod registers a {@link Guard}; a position is skipped when any guard
 * denies it — failing closed exactly where claim information is available.
 */
public final class AutomationPolicy {
    @FunctionalInterface
    public interface Guard {
        boolean denies(ServerLevel level, BlockPos pos, @Nullable UUID owner);
    }

    public static final List<Guard> GUARDS = new CopyOnWriteArrayList<>();

    private AutomationPolicy() { }

    public static boolean mayEdit(ServerLevel level, BlockPos pos, @Nullable UUID owner) {
        for (Guard guard : GUARDS) {
            if (guard.denies(level, pos, owner)) return false;
        }
        return true;
    }
}
