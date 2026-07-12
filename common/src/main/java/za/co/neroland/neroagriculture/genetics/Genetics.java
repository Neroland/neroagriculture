package za.co.neroland.neroagriculture.genetics;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

/**
 * Immutable, self-clamping seed genetics. Every trait is forced into 0..{@link GeneticTrait#MAX_PER_TRAIT}
 * and the total into 0..{@link GeneticTrait#TOTAL_CAP} in the canonical constructor, so no decoded, networked,
 * spliced, mutated, or forged value can ever exceed the caps. Splicing and mutation are deterministic.
 */
public record Genetics(int yield, int speed, int hardiness, int oxygenOutput, int foodPotency) {
    public static final Genetics EMPTY = new Genetics(0, 0, 0, 0, 0);

    public Genetics {
        int[] vals = {clamp(yield), clamp(speed), clamp(hardiness), clamp(oxygenOutput), clamp(foodPotency)};
        int total = vals[0] + vals[1] + vals[2] + vals[3] + vals[4];
        for (int i = vals.length - 1; i >= 0 && total > GeneticTrait.TOTAL_CAP; i--) {
            int take = Math.min(vals[i], total - GeneticTrait.TOTAL_CAP);
            vals[i] -= take;
            total -= take;
        }
        yield = vals[0];
        speed = vals[1];
        hardiness = vals[2];
        oxygenOutput = vals[3];
        foodPotency = vals[4];
    }

    public static final Codec<Genetics> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("yield", 0).forGetter(Genetics::yield),
            Codec.INT.optionalFieldOf("speed", 0).forGetter(Genetics::speed),
            Codec.INT.optionalFieldOf("hardiness", 0).forGetter(Genetics::hardiness),
            Codec.INT.optionalFieldOf("oxygen_output", 0).forGetter(Genetics::oxygenOutput),
            Codec.INT.optionalFieldOf("food_potency", 0).forGetter(Genetics::foodPotency)
    ).apply(instance, Genetics::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, Genetics> STREAM_CODEC = StreamCodec.of(
            (buffer, value) -> {
                buffer.writeVarInt(value.yield);
                buffer.writeVarInt(value.speed);
                buffer.writeVarInt(value.hardiness);
                buffer.writeVarInt(value.oxygenOutput);
                buffer.writeVarInt(value.foodPotency);
            },
            buffer -> new Genetics(buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(),
                    buffer.readVarInt(), buffer.readVarInt()));

    public int total() {
        return yield + speed + hardiness + oxygenOutput + foodPotency;
    }

    public boolean isEmpty() {
        return total() == 0;
    }

    public int get(GeneticTrait trait) {
        return switch (trait) {
            case YIELD -> yield;
            case SPEED -> speed;
            case HARDINESS -> hardiness;
            case OXYGEN_OUTPUT -> oxygenOutput;
            case FOOD_POTENCY -> foodPotency;
        };
    }

    public Genetics with(GeneticTrait trait, int value) {
        return new Genetics(
                trait == GeneticTrait.YIELD ? value : yield,
                trait == GeneticTrait.SPEED ? value : speed,
                trait == GeneticTrait.HARDINESS ? value : hardiness,
                trait == GeneticTrait.OXYGEN_OUTPUT ? value : oxygenOutput,
                trait == GeneticTrait.FOOD_POTENCY ? value : foodPotency);
    }

    /** Deterministic splice: each trait becomes the higher parent value, then the total cap is enforced. */
    public static Genetics splice(Genetics a, Genetics b) {
        return new Genetics(Math.max(a.yield, b.yield), Math.max(a.speed, b.speed), Math.max(a.hardiness, b.hardiness),
                Math.max(a.oxygenOutput, b.oxygenOutput), Math.max(a.foodPotency, b.foodPotency));
    }

    /** Deterministic single-step mutation: raise one trait (chosen from the seed) by one if there is room. */
    public Genetics mutated(long seed) {
        if (total() >= GeneticTrait.TOTAL_CAP) return this;
        GeneticTrait[] traits = GeneticTrait.values();
        for (int i = 0; i < traits.length; i++) {
            GeneticTrait trait = traits[Math.floorMod(seed + i, traits.length)];
            if (get(trait) < GeneticTrait.MAX_PER_TRAIT) return with(trait, get(trait) + 1);
        }
        return this;
    }

    /** Deterministic upgrade: raise the lowest-index trait that still has room by one. */
    public Genetics upgradedLowest() {
        return mutated(0);
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(GeneticTrait.MAX_PER_TRAIT, value));
    }
}
