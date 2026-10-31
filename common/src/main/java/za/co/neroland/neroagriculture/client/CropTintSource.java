package za.co.neroland.neroagriculture.client;

//? if >=26.1 {
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
//?} else {
/*import net.minecraft.client.color.block.BlockColor;
import net.minecraft.world.level.BlockAndTintGetter;
*///?}
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

import za.co.neroland.neroagriculture.catalog.MaterialColors;
import za.co.neroland.neroagriculture.crop.ResourceCropBlockEntity;

/**
 * Tints a placed Resource Crop to its resource's ingot colour (tintindex 0). Reads the crop block
 * entity's material at the position and resolves the colour through {@link MaterialColors} — the same
 * resolver the item tint uses — so in-world crops read as the resource, like the seeds and fragments do.
 * With no block entity/position (e.g. inventory) it returns {@code -1} (no tint).
 */
//? if >=26.1 {
public final class CropTintSource implements BlockTintSource {
    @Override public int color(BlockState state) { return -1; }

    @Override public int colorInWorld(BlockState state, BlockAndTintGetter level, BlockPos pos) {
//?} else {
/*// Before 26.1 block tints are a BlockColor (one callback, level/pos null outside the world).
public final class CropTintSource implements BlockColor {
    @Override public int getColor(BlockState state, @org.jetbrains.annotations.Nullable BlockAndTintGetter level,
            @org.jetbrains.annotations.Nullable BlockPos pos, int tintIndex) {
        return level == null || pos == null ? -1 : colorInWorld(state, level, pos);
    }

    public int colorInWorld(BlockState state, BlockAndTintGetter level, BlockPos pos) {
*///?}
        if (level.getBlockEntity(pos) instanceof ResourceCropBlockEntity crop) {
            return 0xFF000000 | (MaterialColors.resolve(crop.variant().material().getPath()) & 0xFFFFFF);
        }
        return -1;
    }
}
