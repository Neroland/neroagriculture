package za.co.neroland.neroagriculture.tower;

import net.minecraft.resources.Identifier;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.neroagriculture.content.EssenceFamily;
import za.co.neroland.neroagriculture.genetics.Genetics;

/** Mutable virtual-crop state for one tower slot. Empty when no material is planted. */
public final class TowerSlot {
    public static final int MAX_AGE = 7;

    @Nullable private Identifier material;
    private EssenceFamily family = EssenceFamily.TERRAN;
    private int age;
    private int harvestCount;
    private Genetics genetics = Genetics.EMPTY;

    public boolean isEmpty() { return material == null; }
    @Nullable public Identifier material() { return material; }
    public EssenceFamily family() { return family; }
    public int age() { return age; }
    public int harvestCount() { return harvestCount; }
    public Genetics genetics() { return genetics; }
    public boolean mature() { return material != null && age >= MAX_AGE; }

    public void plant(Identifier material, EssenceFamily family, int harvestCount, Genetics genetics) {
        this.material = material;
        this.family = family;
        this.age = 0;
        this.harvestCount = Math.max(0, harvestCount);
        this.genetics = genetics == null ? Genetics.EMPTY : genetics;
    }

    /** Advance growth by a bounded step, clamped to maturity. */
    public void grow(int step) {
        if (material != null) age = Math.min(MAX_AGE, age + Math.max(1, step));
    }

    /** Reset age and record a harvest, keeping the plant in place (like an ordinary harvest). */
    public void harvested() {
        age = 0;
        harvestCount = Math.min(1_000_000_000, harvestCount + 1);
    }

    public void clear() {
        material = null;
        family = EssenceFamily.TERRAN;
        age = 0;
        harvestCount = 0;
        genetics = Genetics.EMPTY;
    }

    public void set(@Nullable Identifier material, EssenceFamily family, int age, int harvestCount, Genetics genetics) {
        this.material = material;
        this.family = family == null ? EssenceFamily.TERRAN : family;
        this.age = Math.max(0, Math.min(MAX_AGE, age));
        this.harvestCount = Math.max(0, harvestCount);
        this.genetics = genetics == null ? Genetics.EMPTY : genetics;
    }
}
