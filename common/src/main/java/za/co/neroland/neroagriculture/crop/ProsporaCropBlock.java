package za.co.neroland.neroagriculture.crop;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerolandcore.registry.BlockCodecs;
import za.co.neroland.neroagriculture.progression.AgricultureMilestones;
import za.co.neroland.neroagriculture.registry.ModItems;

/**
 * The Prospora base crop — a simple vanilla-style crop planted from the Prospora Seed. It grows on
 * farmland like wheat (inheriting {@link CropBlock}'s growth, survival and bonemeal behaviour) and, on
 * harvest, drops Territe Fragments (the tier-1 ladder currency) plus a Prospora Seed, via its loot
 * table — a renewable, standalone entry point to the fragment ladder. Territe is also obtainable from
 * tier-1 ore extraction, so this is a convenience rather than a gate.
 */
public final class ProsporaCropBlock extends CropBlock {
    public static final MapCodec<ProsporaCropBlock> CODEC = BlockCodecs.simple(ProsporaCropBlock::new);

    public ProsporaCropBlock(Properties properties) {
        super(properties);
    }

    public MapCodec<? extends CropBlock> codec() { return CODEC; }

    @Override protected ItemLike getBaseSeedId() { return ModItems.PROSPORA_SEED.get(); }

    /** Breaking a mature Prospora crop is the first harvest many players make; record it (gates nothing). */
    //? if >=26.3 {
    /*@Override public void playerDestroy(net.minecraft.server.level.ServerLevel level, ServerPlayer player, BlockPos pos, BlockState state,
            @Nullable BlockEntity blockEntity, ItemStack destroyedWith) {
    *///?} else {
    @Override public void playerDestroy(net.minecraft.world.level.Level level, net.minecraft.world.entity.player.Player player, BlockPos pos, BlockState state,
            @Nullable BlockEntity blockEntity, ItemStack destroyedWith) {
    //?}
        super.playerDestroy(level, player, pos, state, blockEntity, destroyedWith);
        if (player instanceof ServerPlayer serverPlayer && isMaxAge(state)) {
            AgricultureMilestones.record(serverPlayer, AgricultureMilestones.FIRST_HARVEST);
        }
    }
}
