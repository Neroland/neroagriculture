package za.co.neroland.neroagriculture.registry;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

import za.co.neroland.neroagriculture.NeroAgricultureCommon;
import za.co.neroland.neroagriculture.content.AgricultureMobEffect;
import za.co.neroland.neroagriculture.content.AgricultureMobEffect.Behavior;
import za.co.neroland.nerolandcore.registry.RegistrationProvider;
import za.co.neroland.nerolandcore.registry.RegistrationProvider.RegistryEntry;

/** The three Agriculture-owned signature effects that have no clean vanilla analogue. */
public final class ModMobEffects {
    public static final RegistrationProvider<MobEffect> EFFECTS =
            RegistrationProvider.get(Registries.MOB_EFFECT, NeroAgricultureCommon.MOD_ID);

    public static final RegistryEntry<MobEffect> LOW_GRAVITY_ADAPTATION = EFFECTS.register("low_gravity_adaptation",
            key -> new AgricultureMobEffect(MobEffectCategory.BENEFICIAL, 0x7FD68A, Behavior.LOW_GRAVITY_ADAPTATION));
    public static final RegistryEntry<MobEffect> OXYGEN_EFFICIENCY = EFFECTS.register("oxygen_efficiency",
            key -> new AgricultureMobEffect(MobEffectCategory.BENEFICIAL, 0x4FBF6E, Behavior.OXYGEN_EFFICIENCY));
    public static final RegistryEntry<MobEffect> FREEZE_IMMUNITY = EFFECTS.register("freeze_immunity",
            key -> new AgricultureMobEffect(MobEffectCategory.BENEFICIAL, 0x8FC8E8, Behavior.FREEZE_IMMUNITY));

    private ModMobEffects() { }

    public static Holder<MobEffect> lowGravity() { return holder(LOW_GRAVITY_ADAPTATION); }
    public static Holder<MobEffect> oxygenEfficiency() { return holder(OXYGEN_EFFICIENCY); }
    public static Holder<MobEffect> freezeImmunity() { return holder(FREEZE_IMMUNITY); }

    private static Holder<MobEffect> holder(RegistryEntry<MobEffect> entry) {
        return BuiltInRegistries.MOB_EFFECT.wrapAsHolder(entry.get());
    }

    public static void init() { }
}
