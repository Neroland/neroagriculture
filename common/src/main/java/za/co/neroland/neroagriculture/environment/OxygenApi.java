package za.co.neroland.neroagriculture.environment;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import net.minecraft.resources.Identifier;

/**
 * Public seam for routing greenhouse oxygen production to another mod. When Nerospace is present it registers
 * a consumer to feed its atmosphere/terraforming truth; standalone there are no consumers and the greenhouse
 * simply uses its local life-support model. No player data crosses this seam.
 */
public final class OxygenApi {
    public record Contribution(Identifier dimension, long controllerPos, int amount) { }

    @FunctionalInterface
    public interface Consumer {
        void accept(Contribution contribution);
    }

    public static final List<Consumer> CONSUMERS = new CopyOnWriteArrayList<>();

    private OxygenApi() { }

    public static void contribute(Contribution contribution) {
        for (Consumer consumer : CONSUMERS) consumer.accept(contribution);
    }
}
