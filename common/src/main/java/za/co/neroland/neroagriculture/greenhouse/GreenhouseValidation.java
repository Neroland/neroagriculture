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
        Set<Long> interior = new HashSet<>();
        Deque<BlockPos> queue = new ArrayDeque<>();
        for (Direction direction : Direction.values()) {
            BlockPos seed = controller.relative(direction);
            if (passable.test(seed) && interior.add(seed.asLong())) queue.add(seed);
        }
        if (interior.isEmpty()) return new Result(Structure.UNFORMED, Set.of(), null, 0);
        while (!queue.isEmpty()) {
            BlockPos pos = queue.poll();
            for (Direction direction : Direction.values()) {
                BlockPos next = pos.relative(direction);
                if (next.equals(controller) || !passable.test(next)) continue;
                if (interior.add(next.asLong())) {
                    if (interior.size() > volumeCap) return new Result(Structure.BREACHED, interior, next, interior.size());
                    queue.add(next);
                }
            }
        }
        return new Result(Structure.FORMED, interior, null, interior.size());
    }
}
