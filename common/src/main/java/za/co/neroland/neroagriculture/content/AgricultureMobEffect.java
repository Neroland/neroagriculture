package za.co.neroland.neroagriculture.content;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * Agriculture-owned signature effects with no clean vanilla analogue. Each tick performs a small, bounded,
 * server-authoritative action; behaviour is standalone and never depends on Nerospace being present.
 */
public final class AgricultureMobEffect extends MobEffect {
    public enum Behavior { FREEZE_IMMUNITY, OXYGEN_EFFICIENCY, LOW_GRAVITY_ADAPTATION }

    private final Behavior behavior;

    public AgricultureMobEffect(MobEffectCategory category, int color, Behavior behavior) {
        super(category, color);
        this.behavior = behavior;
    }

    //? if >=1.21.2 {
    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
    //?} else if >=1.20.5 {
    /*@Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
    *///?} else {
    /*@Override
    public void applyEffectTick(LivingEntity entity, int amplifier) {
        if (!entity.level().isClientSide()) {
            apply(entity);
        }
    }

    @Override
    //? if >=1.20.2 {
    //public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
    //?} else {
    public boolean isDurationEffectTick(int duration, int amplifier) {
    //?}
        return true;
    }
    *///?}

    //? if >=1.20.5 {
        apply(entity);
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }
    //?}

    private void apply(LivingEntity entity) {
        switch (behavior) {
            case FREEZE_IMMUNITY -> entity.setTicksFrozen(0);
            case OXYGEN_EFFICIENCY -> {
                if (entity.getAirSupply() < entity.getMaxAirSupply()) entity.setAirSupply(entity.getMaxAirSupply());
            }
            case LOW_GRAVITY_ADAPTATION -> entity.resetFallDistance();
        }
    }
}
