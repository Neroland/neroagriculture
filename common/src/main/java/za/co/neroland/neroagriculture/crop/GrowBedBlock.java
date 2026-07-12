package za.co.neroland.neroagriculture.crop;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import za.co.neroland.neroagriculture.content.EssenceFamily;

/** Powered Industrial–Deepvoid bed. Terran remains a passive plain block. */
public final class GrowBedBlock extends BaseEntityBlock {
    public static final MapCodec<GrowBedBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            EssenceFamily.CODEC.fieldOf("tier").forGetter(GrowBedBlock::tier),
            propertiesCodec()).apply(instance, GrowBedBlock::new));
    private final EssenceFamily tier;

    public GrowBedBlock(EssenceFamily tier, Properties properties) {
        super(properties);
        if (tier == EssenceFamily.TERRAN) throw new IllegalArgumentException("Terran bed is passive");
        this.tier = tier;
    }
    public EssenceFamily tier() { return tier; }
    @Override protected MapCodec<GrowBedBlock> codec() { return CODEC; }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new GrowBedBlockEntity(pos, state); }
}
