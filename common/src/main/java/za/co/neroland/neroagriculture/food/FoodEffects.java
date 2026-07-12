package za.co.neroland.neroagriculture.food;

import java.util.Optional;

import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

import za.co.neroland.neroagriculture.registry.ModMobEffects;

/** Hybrid category → effect resolution and bounded, server-authoritative application on eating. */
public final class FoodEffects {
    private FoodEffects() { }

    public static Optional<Holder<MobEffect>> holder(EffectCategory category) {
        return switch (category) {
            case NONE -> Optional.empty();
            case NIGHT_VISION -> Optional.of(MobEffects.NIGHT_VISION);
            case MINING_HASTE -> Optional.of(MobEffects.HASTE);
            case FIRE_RESISTANCE -> Optional.of(MobEffects.FIRE_RESISTANCE);
            case LOW_GRAVITY_ADAPTATION -> Optional.of(ModMobEffects.lowGravity());
            case OXYGEN_EFFICIENCY -> Optional.of(ModMobEffects.oxygenEfficiency());
            case FREEZE_IMMUNITY -> Optional.of(ModMobEffects.freezeImmunity());
        };
    }

    /** Apply the definition's signature effect, clamped to its caps. Vanilla handles renewal/removal. */
    public static void applyTo(LivingEntity entity, FoodDefinition definition) {
        if (!definition.hasEffect()) return;
        holder(definition.effect()).ifPresent(effect -> entity.addEffect(new MobEffectInstance(effect,
                definition.effectiveDurationTicks(), definition.effectiveAmplifier())));
    }
}
