package za.co.neroland.neroagriculture.genetics;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import za.co.neroland.neroagriculture.automation.AreaWork;
import za.co.neroland.neroagriculture.config.AgricultureConfig;
import za.co.neroland.neroagriculture.crop.SpeciesCropBlock;
import za.co.neroland.neroagriculture.registry.ModBlockEntities;
import za.co.neroland.nerolandcore.machine.AbstractMachineBlockEntity;
import za.co.neroland.nerolandcore.sideconfig.Channel;
import za.co.neroland.nerolandcore.sideconfig.SideConfig;
import za.co.neroland.nerolandcore.sideconfig.SideMode;

/**
 * Optional NF-powered pollination booster. Over a bounded region it drives the same server-authoritative
 * cross-pollination as passive adjacency, just at a higher rate. It is never required and spawns no entities.
 */
public final class PollinationBeaconBlockEntity extends AbstractMachineBlockEntity {
    public static final int ENERGY_PER_PASS = 30;
    private int cursor;
    private int workTimer;

    public PollinationBeaconBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.POLLINATION_BEACON.get(), pos, state, AgricultureConfig.MACHINE_ENERGY_CAPACITY.get(),
                AgricultureConfig.MACHINE_ENERGY_RATE.get(), 0, stack -> null);
        installSideConfig(SideConfig.builder().channel(Channel.ENERGY).allow(Channel.ENERGY, SideMode.OUTPUT, false).build());
        this.workTimer = 1 + AreaWork.phaseOffset(pos, AgricultureConfig.AUTOMATION_INTERVAL.get());
    }

    public static void tick(Level level, BlockPos pos, BlockState state, PollinationBeaconBlockEntity beacon) {
        AbstractMachineBlockEntity.tick(level, pos, state, beacon);
        if (!(level instanceof ServerLevel serverLevel)) return;
        if (--beacon.workTimer > 0) return;
        beacon.workTimer = Math.max(1, AgricultureConfig.AUTOMATION_INTERVAL.get());
        beacon.runPass(serverLevel);
    }

    private void runPass(ServerLevel level) {
        if (getEnergy().extract(ENERGY_PER_PASS, true) < ENERGY_PER_PASS) return;
        int radius = AgricultureConfig.POLLINATION_BEACON_RANGE.get();
        int perPass = AgricultureConfig.AUTOMATION_PER_PASS.get();
        int boosted = Math.min(100, AgricultureConfig.POLLINATION_CHANCE_PERCENT.get() * 4);
        boolean worked = false;
        for (int i = 0; i < perPass; i++) {
            BlockPos column = AreaWork.columnAt(worldPosition, radius, cursor + i);
            for (int dy = -1; dy <= 1; dy++) {
                BlockPos target = column.above(dy);
                if (!level.isLoaded(target)) continue;
                if (level.getBlockState(target).getBlock() instanceof SpeciesCropBlock
                        && CropPollination.attempt(level, target, boosted)) {
                    worked = true;
                }
            }
        }
        cursor = AreaWork.advanceCursor(cursor, perPass, radius);
        if (worked) getEnergy().extract(ENERGY_PER_PASS, false);
    }

    public String status() {
        return "pollination beacon range=" + (2 * AgricultureConfig.POLLINATION_BEACON_RANGE.get() + 1)
                + " NF=" + getEnergy().getAmount();
    }

    @Override protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("Cursor", cursor);
    }
    @Override protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        cursor = Math.max(0, input.getIntOr("Cursor", 0));
    }
}
