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

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
        switch (behavior) {
            case FREEZE_IMMUNITY -> entity.setTicksFrozen(0);
            case OXYGEN_EFFICIENCY -> {
                if (entity.getAirSupply() < entity.getMaxAirSupply()) entity.setAirSupply(entity.getMaxAirSupply());
            }
            case LOW_GRAVITY_ADAPTATION -> entity.resetFallDistance();
        }
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }
}
