package za.co.neroland.neroagriculture.greenhouse;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import org.jetbrains.annotations.Nullable;

/**
 * Pure, bounded flood-fill enclosure detection. Starting from the controller's passable neighbours it fills
 * the interior air pocket, stopping at any solid boundary block. A sealed room is finite: if the fill grows
 * past the volume cap the room is not sealed (or is oversized) and is reported BREACHED with a leak frontier.
 * Runs only on structural change or a slow safety interval — never every tick.
 */
public final class GreenhouseValidation {
    @FunctionalInterface
    public interface Passable {
        boolean test(BlockPos pos);
    }

    public enum Structure { UNFORMED, FORMED, BREACHED }

    public record Result(Structure structure, Set<Long> interior, @Nullable BlockPos leak, int volume) { }

    private GreenhouseValidation() { }

    public static Result validate(BlockPos controller, Passable passable, int volumeCap) {
        Set<Long> visited = new HashSet<>();
        Result best = null;
        Result breach = null;
        for (Direction direction : Direction.values()) {
            BlockPos seed = controller.relative(direction);
            if (!passable.test(seed) || visited.contains(seed.asLong())) continue;
            Set<Long> interior = new HashSet<>();
            Deque<BlockPos> queue = new ArrayDeque<>();
            interior.add(seed.asLong());
            visited.add(seed.asLong());
            queue.add(seed);
            BlockPos leak = null;
            while (!queue.isEmpty() && leak == null) {
                BlockPos pos = queue.poll();
                for (Direction neighbourDirection : Direction.values()) {
                    BlockPos next = pos.relative(neighbourDirection);
                    if (next.equals(controller) || !passable.test(next) || !interior.add(next.asLong())) continue;
                    visited.add(next.asLong());
                    if (interior.size() > volumeCap) {
                        leak = next;
                        break;
                    }
                    queue.add(next);
                }
            }
            Result result = new Result(leak == null ? Structure.FORMED : Structure.BREACHED,
                    interior, leak, interior.size());
            if (leak == null && (best == null || result.volume() > best.volume())) best = result;
            if (leak != null) breach = result;
        }
        if (best != null) return best;
        return breach == null ? new Result(Structure.UNFORMED, Set.of(), null, 0) : breach;
    }
}
