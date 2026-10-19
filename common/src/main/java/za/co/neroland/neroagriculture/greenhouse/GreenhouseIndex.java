package za.co.neroland.neroagriculture.greenhouse;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;

/**
 * Server-thread index of greenhouse interiors so a crop can answer "am I sealed?" in O(1) without scanning
 * for controllers or flood-filling on its own tick. Controllers publish their cached interior here on
 * validation and withdraw it on breach/removal.
 */
public final class GreenhouseIndex {
    private static final Map<Identifier, Map<Long, Long>> INTERIOR_TO_CONTROLLER = new HashMap<>();
    private static final Map<Identifier, Map<Long, Set<Long>>> CONTROLLER_TO_INTERIOR = new HashMap<>();

    private GreenhouseIndex() { }

    public static void publish(ServerLevel level, BlockPos controller, Set<Long> interior) {
        Identifier dim = level.dimension().identifier();
        clear(level, controller);
        Map<Long, Long> forward = INTERIOR_TO_CONTROLLER.computeIfAbsent(dim, key -> new HashMap<>());
        long controllerKey = controller.asLong();
        for (long pos : interior) forward.put(pos, controllerKey);
        CONTROLLER_TO_INTERIOR.computeIfAbsent(dim, key -> new HashMap<>()).put(controllerKey, Set.copyOf(interior));
    }

    public static void clear(ServerLevel level, BlockPos controller) {
        Identifier dim = level.dimension().identifier();
        Map<Long, Set<Long>> controllers = CONTROLLER_TO_INTERIOR.get(dim);
        if (controllers == null) return;
        Set<Long> interior = controllers.remove(controller.asLong());
        if (interior == null) return;
        Map<Long, Long> forward = INTERIOR_TO_CONTROLLER.get(dim);
        if (forward == null) return;
        long controllerKey = controller.asLong();
        for (long pos : interior) forward.remove(pos, controllerKey);
    }

    /** Drop every published interior when the server stops (server-thread, like the rest of the index). */
    public static void clearAll() {
        INTERIOR_TO_CONTROLLER.clear();
        CONTROLLER_TO_INTERIOR.clear();
    }

    /**
     * The controller of the greenhouse whose interior contains {@code pos}, or {@code null} when the
     * position is not inside any published interior.
     *
     * <p>Unlike {@link #sealedAt} this reports the containing greenhouse whatever state it is in — the
     * link module uses it to answer "which greenhouse is my crop in?", and a breached one is exactly the
     * case its owner most wants to hear about. It performs no loaded check and never touches the world,
     * so it cannot force-load a chunk; callers that need the controller's live state must make their own
     * {@code isLoaded} check first, as {@link #sealedAt} does.
     */
    @org.jetbrains.annotations.Nullable
    public static BlockPos controllerAt(ServerLevel level, BlockPos pos) {
        Map<Long, Long> forward = INTERIOR_TO_CONTROLLER.get(level.dimension().identifier());
        if (forward == null) return null;
        Long controllerKey = forward.get(pos.asLong());
        return controllerKey == null ? null : BlockPos.of(controllerKey);
    }

    /** True when the position sits inside a formed, powered greenhouse whose controller is loaded. */
    public static boolean sealedAt(ServerLevel level, BlockPos pos) {
        Map<Long, Long> forward = INTERIOR_TO_CONTROLLER.get(level.dimension().identifier());
        if (forward == null) return false;
        Long controllerKey = forward.get(pos.asLong());
        if (controllerKey == null) return false;
        BlockPos controllerPos = BlockPos.of(controllerKey);
        // Guard the loaded check so a random-tick query can never force-load the controller's chunk.
        return level.isLoaded(controllerPos)
                && level.getBlockEntity(controllerPos) instanceof GreenhouseControllerBlockEntity controller
                && controller.isActive();
    }
}
