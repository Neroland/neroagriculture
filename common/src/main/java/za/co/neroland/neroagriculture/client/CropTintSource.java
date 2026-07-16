package za.co.neroland.neroagriculture.client;

import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
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
public final class CropTintSource implements BlockTintSource {
    @Override public int color(BlockState state) { return -1; }

    @Override public int colorInWorld(BlockState state, BlockAndTintGetter level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof ResourceCropBlockEntity crop) {
            return 0xFF000000 | (MaterialColors.resolve(crop.variant().material().getPath()) & 0xFFFFFF);
        }
        return -1;
    }
}
