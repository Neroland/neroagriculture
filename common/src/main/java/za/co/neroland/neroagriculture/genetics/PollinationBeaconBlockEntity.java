package za.co.neroland.neroagriculture.genetics;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import za.co.neroland.neroagriculture.automation.AreaWork;
import za.co.neroland.neroagriculture.config.AgricultureConfig;
import za.co.neroland.neroagriculture.crop.SpeciesCropBlock;
import za.co.neroland.neroagriculture.machine.SideConfigMigration;
import za.co.neroland.neroagriculture.menu.StatusMenu;
import za.co.neroland.neroagriculture.registry.ModBlockEntities;
import za.co.neroland.nerolandcore.machine.AbstractMachineBlockEntity;
import za.co.neroland.nerolandcore.sideconfig.Channel;
import za.co.neroland.nerolandcore.sideconfig.SideConfig;
import za.co.neroland.nerolandcore.sideconfig.SideMode;
import za.co.neroland.nerolandcore.sideconfig.SidePreset;

/**
 * Optional NF-powered pollination booster. Over a bounded region it drives the same server-authoritative
 * cross-pollination as passive adjacency, just at a higher rate. It is never required and spawns no entities.
 */
public final class PollinationBeaconBlockEntity extends AbstractMachineBlockEntity implements MenuProvider {
    public static final int ENERGY_PER_PASS = 30;
    private int cursor;
    private int workTimer;

    private final ContainerData menuData = new ContainerData() {
        @Override public int get(int index) {
            return switch (index) {
                case StatusMenu.MACHINE_ID -> StatusMenu.ID_BEACON;
                // Permille fraction: ContainerData syncs shorts and the capacity can exceed 32,767.
                case StatusMenu.ENERGY -> za.co.neroland.neroagriculture.menu.GaugeData.permille(
                        getEnergy().getAmount(), getEnergy().getCapacity());
                case StatusMenu.V0 -> 2 * AgricultureConfig.POLLINATION_BEACON_RANGE.get() + 1;
                default -> 0;
            };
        }
        @Override public void set(int index, int value) { }
        @Override public int getCount() { return StatusMenu.DATA_COUNT; }
    };

    @Override public Component getDisplayName() { return getBlockState().getBlock().getName(); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new StatusMenu(id, inventory, menuData, worldPosition, this);
    }

    public PollinationBeaconBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.POLLINATION_BEACON.get(), pos, state, AgricultureConfig.MACHINE_ENERGY_CAPACITY.get(),
                AgricultureConfig.MACHINE_ENERGY_RATE.get(), 0, stack -> null);
        installSideConfig(SideConfig.builder().channel(Channel.ENERGY)
                .defaultPreset(SidePreset.PROCESSOR)
                .allow(Channel.ENERGY, SideMode.OUTPUT, false).build());
        this.workTimer = 1 + AreaWork.phaseOffset(pos, AgricultureConfig.AUTOMATION_INTERVAL.get());
    }

    public static void tick(Level level, BlockPos pos, BlockState state, PollinationBeaconBlockEntity beacon) {
        AbstractMachineBlockEntity.tick(level, pos, state, beacon);
        SideConfigMigration.tick(beacon);
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
        SideConfigMigration.save(output);
    }
    @Override protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        SideConfigMigration.load(this, input);
        cursor = Math.max(0, input.getIntOr("Cursor", 0));
    }
}
