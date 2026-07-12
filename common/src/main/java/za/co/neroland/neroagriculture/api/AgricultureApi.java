package za.co.neroland.neroagriculture.api;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

/** Public, loader-neutral provider/event seams reserved by the Stage 2 foundation. */
public final class AgricultureApi {
    public record DietOutput(Identifier food, int nutrition, float saturation, Identifier profile) { }
    public record Objective(Identifier id, Identifier subject, int target) { }
    public record PremiumGood(Identifier item, long unitPriceNf, Identifier quality) { }
    public record CultivationContext(Identifier crop, Identifier dimension, float growth, float yield) { }
    public record CultivationModifier(float growthMultiplier, float yieldMultiplier) { }
    public record DroneRequest(ServerPlayer player, Identifier task, long blockPosition, int range) { }
    public record BiofuelOffer(Identifier fluid, long amountMb, long energyNf) { }
    public record TerraformingEvent(Identifier project, Identifier dimension, long blockPosition, float progress) { }

    @FunctionalInterface public interface DietProvider { List<DietOutput> outputs(ServerPlayer player); }
    @FunctionalInterface public interface ObjectiveProvider { List<Objective> objectives(ServerPlayer player); }
    @FunctionalInterface public interface PremiumGoodsProvider { List<PremiumGood> goods(ServerPlayer player); }
    @FunctionalInterface public interface CultivationProvider { CultivationModifier modify(CultivationContext context); }
    @FunctionalInterface public interface DroneAssistanceProvider { boolean assist(DroneRequest request); }
    @FunctionalInterface public interface BiofuelConsumer { long accept(BiofuelOffer offer, boolean simulate); }
    @FunctionalInterface public interface TerraformingListener { void onTerraforming(TerraformingEvent event); }

    public static final List<DietProvider> DIET = new CopyOnWriteArrayList<>();
    public static final List<ObjectiveProvider> OBJECTIVES = new CopyOnWriteArrayList<>();
    public static final List<PremiumGoodsProvider> PREMIUM_GOODS = new CopyOnWriteArrayList<>();
    public static final List<CultivationProvider> CULTIVATION = new CopyOnWriteArrayList<>();
    public static final List<DroneAssistanceProvider> DRONES = new CopyOnWriteArrayList<>();
    public static final List<BiofuelConsumer> BIOFUEL = new CopyOnWriteArrayList<>();
    public static final List<TerraformingListener> TERRAFORMING = new CopyOnWriteArrayList<>();

    public static void fireTerraforming(TerraformingEvent event) {
        TERRAFORMING.forEach(listener -> listener.onTerraforming(event));
    }

    private AgricultureApi() { }
}
