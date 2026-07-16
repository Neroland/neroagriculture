package za.co.neroland.neroagriculture.crop;

import com.mojang.serialization.MapCodec;

import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.CropBlock;

import za.co.neroland.neroagriculture.registry.ModItems;

/**
 * The Prospora base crop — a simple vanilla-style crop planted from the Prospora Seed. It grows on
 * farmland like wheat (inheriting {@link CropBlock}'s growth, survival and bonemeal behaviour) and, on
 * harvest, drops Territe Fragments (the tier-1 ladder currency) plus a Prospora Seed, via its loot
 * table — a renewable, standalone entry point to the fragment ladder. Territe is also obtainable from
 * tier-1 ore extraction, so this is a convenience rather than a gate.
 */
public final class ProsporaCropBlock extends CropBlock {
    public static final MapCodec<ProsporaCropBlock> CODEC = simpleCodec(ProsporaCropBlock::new);

    public ProsporaCropBlock(Properties properties) {
        super(properties);
    }

    @Override public MapCodec<? extends CropBlock> codec() { return CODEC; }

    @Override protected ItemLike getBaseSeedId() { return ModItems.PROSPORA_SEED.get(); }
}
