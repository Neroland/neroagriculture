package za.co.neroland.neroagriculture.machine;

import net.minecraft.world.level.block.Block;

import za.co.neroland.neroagriculture.registry.ModBlocks;

/** Stable behavior selected from the finite machine block registry. */
public enum MachineKind {
    EXTRACTOR, INFUSER, SYNTHESIZER, RESEARCH_BENCH, OTHER;

    public static MachineKind of(Block block) {
        if (block == ModBlocks.ESSENCE_EXTRACTOR.get()) return EXTRACTOR;
        if (block == ModBlocks.ESSENCE_INFUSER.get()) return INFUSER;
        if (block == ModBlocks.SEED_SYNTHESIZER.get()) return SYNTHESIZER;
        if (block == ModBlocks.SEED_RESEARCH_BENCH.get()) return RESEARCH_BENCH;
        return OTHER;
    }
}
