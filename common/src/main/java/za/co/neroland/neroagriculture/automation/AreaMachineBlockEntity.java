package za.co.neroland.neroagriculture.automation;

import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.neroagriculture.config.AgricultureConfig;
import za.co.neroland.neroagriculture.content.AgricultureUpgradeItem;
import za.co.neroland.neroagriculture.machine.SideConfigMigration;
import za.co.neroland.neroagriculture.menu.AreaMachineMenu;
import za.co.neroland.neroagriculture.registry.ModBlockEntities;
import za.co.neroland.neroagriculture.registry.ModBlocks;
import za.co.neroland.neroagriculture.registry.ModItems;
import za.co.neroland.nerolandcore.machine.AbstractMachineBlockEntity;
import za.co.neroland.nerolandcore.sideconfig.Channel;
import za.co.neroland.nerolandcore.sideconfig.SideConfig;
import za.co.neroland.nerolandcore.sideconfig.SideMode;
import za.co.neroland.nerolandcore.sideconfig.SidePreset;
import za.co.neroland.nerolandcore.upgrade.UpgradeType;

/** Bounded, interval-batched Planter/Harvester. One BE serves both modes, selected from its block. */
public final class AreaMachineBlockEntity extends AbstractMachineBlockEntity
        implements WorldlyContainer, AutomationOwner.Owned, MenuProvider {
    public enum Mode { PLANT, HARVEST, APPLY }

    public static final int SLOT_COUNT = 9;
    public static final int UPGRADE_START = 9;
    public static final int UPGRADE_SLOTS = 3;
    private static final int[] SLOTS = {0, 1, 2, 3, 4, 5, 6, 7, 8};

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    @Nullable private UUID owner;
    private int cursor;
    private int workTimer;

    public AreaMachineBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.AREA_MACHINE.get(), pos, state, AgricultureConfig.MACHINE_ENERGY_CAPACITY.get(),
                AgricultureConfig.MACHINE_ENERGY_RATE.get(), UPGRADE_SLOTS, AgricultureUpgradeItem.CLASSIFIER);
        installSideConfig(SideConfig.builder()
                .channel(Channel.ITEM, za.co.neroland.nerolandcore.sideconfig.SlotGroup.of("input", SLOTS),
                        za.co.neroland.nerolandcore.sideconfig.SlotGroup.of("output", SLOTS))
                .channel(Channel.ENERGY)
                .defaultPreset(SidePreset.PROCESSOR)
                .allow(Channel.ENERGY, SideMode.OUTPUT, false).build())
                .withItems(() -> this);
        this.workTimer = 1 + AreaWork.phaseOffset(pos, AgricultureConfig.AUTOMATION_INTERVAL.get());
    }

    public Mode mode() {
        Block block = getBlockState().getBlock();
        if (block == ModBlocks.HARVESTER.get()) return Mode.HARVEST;
        if (block == ModBlocks.FERTILISER_APPLICATOR.get()) return Mode.APPLY;
        return Mode.PLANT;
    }

    private boolean showArea;

    // The energy slot ships as a permille fraction: ContainerData syncs shorts, and the configured
    // energy capacity can exceed 32,767 (see menu.GaugeData).
    private final ContainerData menuData = new ContainerData() {
        @Override public int get(int index) {
            return switch (index) {
                case 0 -> mode().ordinal();
                case 1 -> za.co.neroland.neroagriculture.menu.GaugeData.permille(
                        getEnergy().getAmount(), getEnergy().getCapacity());
                case 2 -> 2 * AreaWork.radius(upgrades.count(UpgradeType.RANGE)) + 1;
                case 3 -> showArea ? 1 : 0;
                default -> 0;
            };
        }
        @Override public void set(int index, int value) { }
        @Override public int getCount() { return 4; }
    };

    public void toggleShowArea() {
        showArea = !showArea;
        setChanged();
    }

    @Override public Component getDisplayName() { return getBlockState().getBlock().getName(); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            za.co.neroland.neroagriculture.network.AgricultureNetwork.sendToPlayer(serverPlayer,
                    new za.co.neroland.neroagriculture.network.MachineMenuPositionPayload(id, worldPosition.asLong()));
        }
        return new AreaMachineMenu(id, inventory, this, menuData, worldPosition);
    }

    @Override public @Nullable UUID automationOwner() { return owner; }
    @Override public void clearAutomationOwner() { owner = null; setChanged(); }
    public void setOwner(@Nullable UUID owner) {
        this.owner = AutomationOwner.trackingEnabled() ? owner : null;
        setChanged();
    }

    public static void tick(Level level, BlockPos pos, BlockState state, AreaMachineBlockEntity machine) {
        AbstractMachineBlockEntity.tick(level, pos, state, machine);
        SideConfigMigration.tick(machine);
        if (!(level instanceof ServerLevel serverLevel)) return;
        if (machine.showArea && level.getGameTime() % 10 == 0) machine.emitAreaHologram(serverLevel);
        if (--machine.workTimer > 0) return;
        machine.workTimer = Math.max(1, AgricultureConfig.AUTOMATION_INTERVAL.get());
        machine.runPass(serverLevel);
    }

    /** "Hologram" outline of the working area: end-rod particles along the square perimeter. */
    private void emitAreaHologram(ServerLevel level) {
        int radius = AreaWork.radius(upgrades.count(UpgradeType.RANGE));
        double y = worldPosition.getY() + 1.15;
        for (int d = -radius; d <= radius; d++) {
            spawnMarker(level, worldPosition.getX() + d + 0.5, y, worldPosition.getZ() - radius + 0.5);
            spawnMarker(level, worldPosition.getX() + d + 0.5, y, worldPosition.getZ() + radius + 0.5);
            spawnMarker(level, worldPosition.getX() - radius + 0.5, y, worldPosition.getZ() + d + 0.5);
            spawnMarker(level, worldPosition.getX() + radius + 0.5, y, worldPosition.getZ() + d + 0.5);
        }
    }

    private static void spawnMarker(ServerLevel level, double x, double y, double z) {
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD, x, y, z, 1, 0, 0, 0, 0);
    }

    private void runPass(ServerLevel level) {
        int perPass = AgricultureConfig.AUTOMATION_PER_PASS.get();
        int radius = AreaWork.radius(upgrades.count(UpgradeType.RANGE));
        int energyPerOp = AgricultureConfig.AUTOMATION_ENERGY_PER_OP.get();
        Mode mode = mode();
        for (int i = 0; i < perPass; i++) {
            BlockPos bed = AreaWork.columnAt(worldPosition, radius, cursor + i);
            if (bed.getX() == worldPosition.getX() && bed.getZ() == worldPosition.getZ()) continue;
            if (!level.isLoaded(bed)) continue;
            if (!AutomationPolicy.mayEdit(level, bed, owner)) continue;
            if (energyPerOp > 0 && getEnergy().extract(energyPerOp, true) < energyPerOp) break;
            boolean worked = switch (mode) {
                case PLANT -> tryPlant(level, bed);
                case HARVEST -> tryHarvest(level, bed);
                case APPLY -> tryApply(level, bed);
            };
            if (worked && energyPerOp > 0) getEnergy().extract(energyPerOp, false);
        }
        cursor = AreaWork.advanceCursor(cursor, perPass, radius);
        setChanged();
    }

    private boolean tryPlant(ServerLevel level, BlockPos bed) {
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            ItemStack seed = items.get(slot);
            if (seed.isEmpty()) continue;
            if (AreaFarming.plant(level, bed, seed, gatePlayer(level))) return true;
        }
        return false;
    }

    private boolean tryHarvest(ServerLevel level, BlockPos bed) {
        BlockPos cropPos = bed.above();
        if (!AreaFarming.isMature(level, cropPos)) return false;
        return AreaFarming.harvest(level, cropPos, gatePlayer(level), 0, this::acceptOutput);
    }

    private boolean tryApply(ServerLevel level, BlockPos bed) {
        if (!(level.getBlockEntity(bed) instanceof za.co.neroland.neroagriculture.fertiliser.FertilisableBed target)) return false;
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            ItemStack stack = items.get(slot);
            za.co.neroland.neroagriculture.fertiliser.FertiliserType type = fertiliserType(stack);
            if (type == null) continue;
            long now = level.getGameTime();
            target.applyFertiliser(type, 1, now, AgricultureConfig.FERTILISER_DURATION_TICKS.get(),
                    AgricultureConfig.FERTILISER_MAX_DOSE.get());
            stack.shrink(1);
            return true;
        }
        return false;
    }

    @Nullable
    private static za.co.neroland.neroagriculture.fertiliser.FertiliserType fertiliserType(ItemStack stack) {
        if (stack.is(ModItems.SPEED_FERTILISER.get())) return za.co.neroland.neroagriculture.fertiliser.FertiliserType.SPEED;
        if (stack.is(ModItems.YIELD_FERTILISER.get())) return za.co.neroland.neroagriculture.fertiliser.FertiliserType.YIELD;
        return null;
    }

    private void acceptOutput(ItemStack stack) {
        ItemStack remaining = stack;
        for (int slot = 0; slot < SLOT_COUNT && !remaining.isEmpty(); slot++) {
            ItemStack existing = items.get(slot);
            if (existing.isEmpty()) {
                items.set(slot, remaining.copyWithCount(Math.min(remaining.getCount(), remaining.getMaxStackSize())));
                remaining = remaining.getCount() > remaining.getMaxStackSize()
                        ? remaining.copyWithCount(remaining.getCount() - remaining.getMaxStackSize()) : ItemStack.EMPTY;
            } else if (ItemStack.isSameItemSameComponents(existing, remaining)) {
                int move = Math.min(remaining.getCount(), existing.getMaxStackSize() - existing.getCount());
                existing.grow(move);
                remaining = remaining.copyWithCount(remaining.getCount() - move);
            }
        }
        if (!remaining.isEmpty()) Block.popResource(level, worldPosition.above(), remaining);
    }

    @Nullable
    private ServerPlayer gatePlayer(ServerLevel level) {
        if (owner != null) {
            ServerPlayer online = level.getServer().getPlayerList().getPlayer(owner);
            if (online != null) return online;
        }
        Player nearest = level.getNearestPlayer(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5,
                worldPosition.getZ() + 0.5, 48.0, false);
        return nearest instanceof ServerPlayer serverPlayer ? serverPlayer : null;
    }

    // --- container ---------------------------------------------------------
    @Override public int getContainerSize() { return SLOT_COUNT + UPGRADE_SLOTS; }
    @Override public boolean isEmpty() {
        return items.stream().allMatch(ItemStack::isEmpty) && upgrades.items().stream().allMatch(ItemStack::isEmpty);
    }
    @Override public ItemStack getItem(int slot) {
        return slot < SLOT_COUNT ? items.get(slot) : upgrades.getStack(slot - UPGRADE_START);
    }
    @Override public ItemStack removeItem(int slot, int amount) {
        ItemStack out = slot < SLOT_COUNT ? ContainerHelper.removeItem(items, slot, amount)
                : upgrades.getStack(slot - UPGRADE_START).split(amount);
        if (!out.isEmpty()) setChanged();
        return out;
    }
    @Override public ItemStack removeItemNoUpdate(int slot) {
        if (slot < SLOT_COUNT) return ContainerHelper.takeItem(items, slot);
        ItemStack out = upgrades.getStack(slot - UPGRADE_START);
        upgrades.setStack(slot - UPGRADE_START, ItemStack.EMPTY);
        return out;
    }
    @Override public void setItem(int slot, ItemStack stack) {
        if (slot < SLOT_COUNT) items.set(slot, stack);
        else upgrades.setStack(slot - UPGRADE_START, stack);
        setChanged();
    }
    @Override public boolean stillValid(Player player) {
        return !isRemoved() && player.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5,
                worldPosition.getZ() + 0.5) <= 64.0;
    }
    @Override public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot >= UPGRADE_START) return upgrades.isModule(stack);
        return acceptsInput(stack);
    }
    @Override public int[] getSlotsForFace(Direction side) { return SLOTS.clone(); }
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return slot < SLOT_COUNT && acceptsInput(stack);
    }

    private boolean acceptsInput(ItemStack stack) {
        return switch (mode()) {
            case PLANT -> isSeed(stack);
            case APPLY -> fertiliserType(stack) != null;
            case HARVEST -> false;
        };
    }
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return mode() == Mode.HARVEST && slot < SLOT_COUNT;
    }
    @Override public void clearContent() {
        items.clear();
        for (int i = 0; i < upgrades.slots(); i++) upgrades.setStack(i, ItemStack.EMPTY);
        setChanged();
    }

    private static boolean isSeed(ItemStack stack) {
        return stack.is(ModItems.RESOURCE_SEED.get()) || stack.is(ModItems.FOOD_SEED.get())
                || stack.is(ModItems.ALIEN_SEED.get());
    }

    /** Insert a held item into a matching slot (seed or upgrade); returns the leftover. */
    public ItemStack insertHeld(ItemStack held) {
        if (upgrades.isModule(held)) {
            for (int i = 0; i < upgrades.slots(); i++) {
                if (upgrades.getStack(i).isEmpty()) {
                    upgrades.setStack(i, held.copyWithCount(1));
                    setChanged();
                    return shrunk(held);
                }
            }
            return held;
        }
        if (acceptsInput(held)) {
            for (int slot = 0; slot < SLOT_COUNT; slot++) {
                if (items.get(slot).isEmpty()) {
                    items.set(slot, held.copy());
                    setChanged();
                    return ItemStack.EMPTY;
                }
            }
        }
        return held;
    }

    private static ItemStack shrunk(ItemStack held) {
        ItemStack copy = held.copy();
        copy.shrink(1);
        return copy;
    }

    public String status() {
        int radius = AreaWork.radius(upgrades.count(UpgradeType.RANGE));
        int side = 2 * radius + 1;
        return mode().name().toLowerCase() + " area=" + side + "x" + side + " NF=" + getEnergy().getAmount()
                + " owner=" + (owner == null ? "none" : "set") + " speed=x" + modifiers().speedMultiplier();
    }

    @Override protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, items);
        output.putInt("Cursor", cursor);
        output.putBoolean("ShowArea", showArea);
        AutomationOwner.save(output, owner);
        SideConfigMigration.save(output);
    }

    @Override protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        SideConfigMigration.load(this, input);
        ContainerHelper.loadAllItems(input, items);
        cursor = Math.max(0, input.getIntOr("Cursor", 0));
        showArea = input.getBooleanOr("ShowArea", false);
        owner = AutomationOwner.load(input);
    }

    @Override public void setRemoved() { AutomationOwner.untrack(this); super.setRemoved(); }
    @Override public void clearRemoved() {
        super.clearRemoved();
        AutomationOwner.track(this);
        if (owner != null && level instanceof ServerLevel serverLevel) {
            owner = ErasedOwners.filter(owner, serverLevel.getServer());
        }
    }

    /** Breaking/replacing the machine (any cause, creative and explosions included) drops the contents. */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (this.level instanceof ServerLevel) net.minecraft.world.Containers.dropContents(this.level, pos, this);
    }
}
