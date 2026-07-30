package za.co.neroland.neroagriculture.greenhouse;

import com.mojang.serialization.MapCodec;

import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.properties.BlockSetType;

/**
 * The greenhouse airlock: a 2-block-tall, hand-openable metal-and-glass door for the greenhouse shell.
 * Vanilla {@link DoorBlock} supplies placement (both halves), open/close interaction, hitboxes and
 * pathfinding; {@link BlockSetType#COPPER} makes it openable by hand (no redstone needed) with metal
 * door sounds.
 *
 * <p><b>Seal semantics — airlock:</b> the greenhouse enclosure check ({@link GreenhouseValidation} via
 * {@link GreenhouseControllerBlockEntity}) treats both door halves as valid sealing shell blocks whether
 * the door is open or closed, so walking in and out never breaches the greenhouse. The interior flood
 * fill stops at the door block itself in every state.
 */
public final class GreenhouseDoorBlock extends DoorBlock {
    public static final MapCodec<GreenhouseDoorBlock> CODEC = simpleCodec(GreenhouseDoorBlock::new);

    public GreenhouseDoorBlock(Properties properties) {
        super(BlockSetType.COPPER, properties);
    }

    @Override
    public MapCodec<? extends DoorBlock> codec() {
        return CODEC;
    }
}
