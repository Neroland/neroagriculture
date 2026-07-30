package za.co.neroland.neroagriculture.content;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import za.co.neroland.neroagriculture.registry.ModBlocks;

/**
 * The Prospora Seed. Besides being the crafting core of every resource seed, it plants the Prospora
 * base crop on farmland (like wheat seeds) — the crop grows and drops Territe Fragments on harvest.
 */
public final class ProsporaSeedItem extends Item {
    public ProsporaSeedItem(Properties properties) { super(properties); }

    @Override public InteractionResult useOn(UseOnContext context) {
        if (context.getClickedFace() != Direction.UP) return InteractionResult.PASS;
        Level level = context.getLevel();
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        BlockPos cropPos = context.getClickedPos().above();
        BlockState crop = ModBlocks.PROSPORA_CROP.get().defaultBlockState();
        if (!level.getBlockState(cropPos).canBeReplaced() || !crop.canSurvive(level, cropPos)) {
            return InteractionResult.PASS;
        }
        level.setBlock(cropPos, crop, 3);
        Player player = context.getPlayer();
        if (player == null || !player.getAbilities().instabuild) context.getItemInHand().shrink(1);
        return InteractionResult.SUCCESS;
    }
}
