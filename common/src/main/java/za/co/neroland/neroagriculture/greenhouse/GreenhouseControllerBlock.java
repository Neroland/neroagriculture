package za.co.neroland.neroagriculture.greenhouse;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.neroagriculture.registry.ModBlockEntities;

/** Controller for the sealed greenhouse multiblock. Right-click prints its status without opening a menu. */
public final class GreenhouseControllerBlock extends BaseEntityBlock {
    public static final MapCodec<GreenhouseControllerBlock> CODEC = simpleCodec(GreenhouseControllerBlock::new);

    public GreenhouseControllerBlock(Properties properties) {
        super(properties);
    }

    @Override protected MapCodec<GreenhouseControllerBlock> codec() { return CODEC; }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new GreenhouseControllerBlockEntity(pos, state); }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
            BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (level.getBlockEntity(pos) instanceof GreenhouseControllerBlockEntity controller) {
            String leak = controller.leak() == null ? "none" : controller.leak().toShortString();
            player.sendSystemMessage(Component.literal("Greenhouse " + controller.state().name().toLowerCase()
                    + " volume=" + controller.volume() + " crops=" + controller.activeCrops()
                    + " NF=" + controller.getEnergy().getAmount() + " nutrient=" + controller.getFluid().getAmount()
                    + "mb leak=" + leak));
        }
        return InteractionResult.SUCCESS;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
            BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, ModBlockEntities.GREENHOUSE_CONTROLLER.get(),
                GreenhouseControllerBlockEntity::tick);
    }
}
