package za.co.neroland.neroagriculture.network;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.core.BlockPos;

/** Small client mailbox keyed by bounded vanilla container id. */
public final class ClientMachineMenuPositions {
    private static final Map<Integer, BlockPos> POSITIONS = new ConcurrentHashMap<>();
    private ClientMachineMenuPositions() { }
    public static void accept(MachineMenuPositionPayload payload) {
        if (payload.containerId() >= 0 && payload.containerId() <= 100_000) {
            POSITIONS.put(payload.containerId(), payload.blockPos());
        }
    }
    public static BlockPos poll(int containerId) { return POSITIONS.remove(containerId); }
}
