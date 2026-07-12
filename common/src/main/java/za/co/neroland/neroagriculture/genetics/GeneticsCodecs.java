package za.co.neroland.neroagriculture.genetics;

import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Compact block-entity persistence for {@link Genetics}; decode always re-clamps through the constructor. */
public final class GeneticsCodecs {
    private GeneticsCodecs() { }

    public static void save(ValueOutput output, Genetics genetics) {
        if (genetics == null || genetics.isEmpty()) return;
        output.putInt("GenY", genetics.yield());
        output.putInt("GenSp", genetics.speed());
        output.putInt("GenHa", genetics.hardiness());
        output.putInt("GenOx", genetics.oxygenOutput());
        output.putInt("GenFp", genetics.foodPotency());
    }

    public static Genetics load(ValueInput input) {
        return new Genetics(input.getIntOr("GenY", 0), input.getIntOr("GenSp", 0), input.getIntOr("GenHa", 0),
                input.getIntOr("GenOx", 0), input.getIntOr("GenFp", 0));
    }
}
