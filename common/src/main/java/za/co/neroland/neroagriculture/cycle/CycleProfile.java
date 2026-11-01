package za.co.neroland.neroagriculture.cycle;

import java.util.List;

import net.minecraft.resources.Identifier;

/**
 * A datapack-driven cycle for one dimension: an ordered list of phases (seasons/stellar states) that repeat
 * over {@code period} ticks, shifted by {@code phaseOffset}. The current phase is a pure function of world
 * time, so restarts, /time changes, sleeping and offline chunks all resolve to the same deterministic phase
 * — time can never be used to duplicate output.
 */
public record CycleProfile(Identifier dimension, long period, long phaseOffset, List<Phase> phases) {

    public record Phase(String displayKey, CycleModifier modifier) { }

    public CycleProfile {
        if (period < 1) throw new IllegalArgumentException("cycle period must be >= 1");
        if (phases == null || phases.isEmpty()) throw new IllegalArgumentException("cycle needs at least one phase");
        phases = List.copyOf(phases);
    }

    private long normalised(long time) {
        return Math.floorMod(time + phaseOffset, period);
    }

    public int phaseIndex(long time) {
        long span = Math.max(1, period / phases.size());
        return (int) Math.max(0, Math.min(phases.size() - 1, normalised(time) / span));
    }

    public Phase phaseAt(long time) {
        return phases.get(phaseIndex(time));
    }

    public CycleModifier modifierAt(long time) {
        return phaseAt(time).modifier();
    }

    /** Ticks until the next phase begins (for forecast text). */
    public long ticksUntilNextPhase(long time) {
        long span = Math.max(1, period / phases.size());
        long position = normalised(time);
        int index = phaseIndex(time);
        long boundary = index == phases.size() - 1 ? period : Math.min(period, (index + 1L) * span);
        return boundary - position;
    }

    public Phase nextPhase(long time) {
        int index = phaseIndex(time);
        long span = Math.max(1, period / phases.size());
        return phases.get(index == phases.size() - 1 || (index + 1L) * span >= period ? 0 : index + 1);
    }
}
