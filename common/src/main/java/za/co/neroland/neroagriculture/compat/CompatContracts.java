package za.co.neroland.neroagriculture.compat;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import za.co.neroland.neroagriculture.api.AgricultureApi;
import za.co.neroland.neroagriculture.compat.nerospace.NerospaceVisitBridge;
import za.co.neroland.neroagriculture.cycle.CycleApi;
import za.co.neroland.neroagriculture.cycle.CycleModifier;
import za.co.neroland.neroagriculture.environment.EnvironmentApi;
import za.co.neroland.neroagriculture.environment.EnvironmentProfile;
import za.co.neroland.neroagriculture.environment.OxygenApi;

/**
 * Single, loader-neutral entry point for integrating with NeroAgriculture. Every route below is optional and
 * dormant until another mod registers into it — with nothing registered the mod runs fully standalone. No
 * third-party classes are referenced here or anywhere in {@code common/}; external mods interoperate only
 * through Core capabilities, {@code c:} common tags, and these seams. That is why NeroAgriculture keeps a hard
 * dependency on Neroland Core alone.
 */
public final class CompatContracts {
    private CompatContracts() { }

    // --- Power (Nerotech / NeroPower / Energized Power) --------------------
    public static void registerBiofuelConsumer(AgricultureApi.BiofuelConsumer consumer) { AgricultureApi.BIOFUEL.add(consumer); }
    public static void removeBiofuelConsumer(AgricultureApi.BiofuelConsumer consumer) { AgricultureApi.BIOFUEL.remove(consumer); }
    public static boolean hasBiofuelConsumer() { return !AgricultureApi.BIOFUEL.isEmpty(); }

    // --- Environment / atmosphere (Nerospace / Ad Astra) ------------------
    public static void registerEnvironmentProvider(EnvironmentApi.WorldEnvironmentProvider provider) { EnvironmentApi.PROVIDERS.add(provider); }
    public static void removeEnvironmentProvider(EnvironmentApi.WorldEnvironmentProvider provider) { EnvironmentApi.PROVIDERS.remove(provider); }
    public static boolean hasEnvironmentProvider() { return !EnvironmentApi.PROVIDERS.isEmpty(); }

    public static void registerOxygenConsumer(OxygenApi.Consumer consumer) { OxygenApi.CONSUMERS.add(consumer); }
    public static void removeOxygenConsumer(OxygenApi.Consumer consumer) { OxygenApi.CONSUMERS.remove(consumer); }

    // --- Weather / events (Nerospace / NeroEvents) ------------------------
    public static void registerCycleProvider(CycleApi.Provider provider) { CycleApi.PROVIDERS.add(provider); }
    public static void removeCycleProvider(CycleApi.Provider provider) { CycleApi.PROVIDERS.remove(provider); }

    // --- Nerospace planet visits (runtime-guarded, no compile-time dep) ---
    // Each loader entry point wires its player join / dimension-change events into these hooks; the
    // bridge grants Core material_discovered milestones for planet-bound materials. Dormant (one
    // config read + one namespace compare) when Nerospace is absent or compat.nerospace_visits=false.
    public static void playerJoined(ServerPlayer player) { NerospaceVisitBridge.onPlayerJoin(player); }
    public static void playerChangedDimension(ServerPlayer player) { NerospaceVisitBridge.onDimensionChange(player); }
    /** Tick-driven fallback for loaders without a server-side dimension-change event (Fabric). */
    public static void serverTick(net.minecraft.server.MinecraftServer server) { NerospaceVisitBridge.onServerTick(server); }
    public static void playerDisconnected(ServerPlayer player) { NerospaceVisitBridge.playerDisconnected(player); }

    // --- Assistance (NeroCreatures / drones) ------------------------------
    public static void registerDroneProvider(AgricultureApi.DroneAssistanceProvider provider) { AgricultureApi.DRONES.add(provider); }
    public static void removeDroneProvider(AgricultureApi.DroneAssistanceProvider provider) { AgricultureApi.DRONES.remove(provider); }

    // --- Colonies / Economy / Quests consumers ----------------------------
    public static void registerDietProvider(AgricultureApi.DietProvider provider) { AgricultureApi.DIET.add(provider); }
    public static void registerObjectiveProvider(AgricultureApi.ObjectiveProvider provider) { AgricultureApi.OBJECTIVES.add(provider); }
    public static void registerPremiumGoodsProvider(AgricultureApi.PremiumGoodsProvider provider) { AgricultureApi.PREMIUM_GOODS.add(provider); }
    public static void registerCultivationProvider(AgricultureApi.CultivationProvider provider) { AgricultureApi.CULTIVATION.add(provider); }
    public static void registerTerraformingListener(AgricultureApi.TerraformingListener listener) { AgricultureApi.TERRAFORMING.add(listener); }

    // --- Convenience defaults used by the mod when no provider is present --
    /** The effective world environment: a registered provider's value, else the local model's default. */
    public static EnvironmentProfile environmentOrDefault(EnvironmentApi.WorldEnvironmentProvider fallbackProvider,
            ServerLevel level, BlockPos pos) {
        for (EnvironmentApi.WorldEnvironmentProvider provider : EnvironmentApi.PROVIDERS) {
            var supplied = provider.at(level, pos);
            if (supplied.isPresent()) return supplied.get();
        }
        return fallbackProvider.at(level, pos).orElse(EnvironmentProfile.HABITABLE);
    }

    /** The external cycle contribution — exactly identity (1.0) when no provider is registered. */
    public static CycleModifier externalCycle(Identifier dimension, long time) {
        CycleModifier modifier = CycleModifier.IDENTITY;
        for (CycleApi.Provider provider : CycleApi.PROVIDERS) modifier = modifier.combine(provider.modifier(dimension, time));
        return modifier;
    }
}
