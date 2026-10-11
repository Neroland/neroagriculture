package za.co.neroland.neroagriculture.api;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

/** Public, loader-neutral provider/event seams reserved by the Stage 2 foundation. */
public final class AgricultureApi {
    public record DietOutput(Identifier food, int nutrition, float saturation, Identifier profile) { }
    public record Objective(Identifier id, Identifier subject, int target) { }
    /**
     * @deprecated Part of the unused {@link PremiumGoodsProvider} seam. {@code unitPriceNf} is energy, not
     * credits. Pricing belongs in data: key commodities on the item tags {@code #neroagriculture:fragments},
     * {@code #neroagriculture:fertilisers} and {@code #neroagriculture:biofuels} (and the {@code c:} tags
     * the items also carry) instead. Kept only so existing callers still compile; nothing reads it, and it
     * may be removed in a future major version.
     */
    @Deprecated
    public record PremiumGood(Identifier item, long unitPriceNf, Identifier quality) { }
    public record CultivationContext(Identifier crop, Identifier dimension, float growth, float yield) { }
    public record CultivationModifier(float growthMultiplier, float yieldMultiplier) { }
    public record DroneRequest(ServerPlayer player, Identifier task, long blockPosition, int range) { }
    public record BiofuelOffer(Identifier fluid, long amountMb, long energyNf) { }
    public record TerraformingEvent(Identifier project, Identifier dimension, long blockPosition, float progress) { }

    @FunctionalInterface public interface DietProvider { List<DietOutput> outputs(ServerPlayer player); }
    @FunctionalInterface public interface ObjectiveProvider { List<Objective> objectives(ServerPlayer player); }
    /**
     * @deprecated Never implemented and never read. A provider keyed by {@link ServerPlayer} would hand
     * player state to a pricing mod for no reason. Pricing belongs in data: key commodities on the item tags {@code #neroagriculture:fragments},
     * {@code #neroagriculture:fertilisers} and {@code #neroagriculture:biofuels} (and the {@code c:} tags
     * the items also carry) instead. Kept only so existing callers still compile; nothing reads it, and it
     * may be removed in a future major version.
     */
    @Deprecated
    @FunctionalInterface public interface PremiumGoodsProvider { List<PremiumGood> goods(ServerPlayer player); }
    @FunctionalInterface public interface CultivationProvider { CultivationModifier modify(CultivationContext context); }
    @FunctionalInterface public interface DroneAssistanceProvider { boolean assist(DroneRequest request); }
    @FunctionalInterface public interface BiofuelConsumer { long accept(BiofuelOffer offer, boolean simulate); }
    @FunctionalInterface public interface TerraformingListener { void onTerraforming(TerraformingEvent event); }

    public static final List<DietProvider> DIET = new CopyOnWriteArrayList<>();
    public static final List<ObjectiveProvider> OBJECTIVES = new CopyOnWriteArrayList<>();
    /** @deprecated See {@link PremiumGoodsProvider}: nothing reads this list. Use the item tags instead. */
    @Deprecated
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
