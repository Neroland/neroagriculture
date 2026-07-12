package za.co.neroland.neroagriculture.tower;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.neroagriculture.automation.AreaWork;
import za.co.neroland.neroagriculture.balance.TierBalance;
import za.co.neroland.neroagriculture.catalog.MaterialCatalog;
import za.co.neroland.neroagriculture.config.AgricultureConfig;
import za.co.neroland.neroagriculture.content.EssenceFamily;
import za.co.neroland.neroagriculture.content.MaterialVariant;
import za.co.neroland.neroagriculture.crop.YieldCurve;
import za.co.neroland.neroagriculture.fluid.ModFluids;
import za.co.neroland.neroagriculture.genetics.GeneticEffects;
import za.co.neroland.neroagriculture.genetics.Genetics;
import za.co.neroland.neroagriculture.registry.ModBlockEntities;
import za.co.neroland.neroagriculture.registry.ModBlocks;
import za.co.neroland.neroagriculture.registry.ModDataComponents;
import za.co.neroland.neroagriculture.registry.ModItems;
import za.co.neroland.nerolandcore.fluid.FluidBuffer;
import za.co.neroland.nerolandcore.fluid.NeroFluidStorage;
import za.co.neroland.nerolandcore.machine.AbstractMachineBlockEntity;
import za.co.neroland.nerolandcore.progression.ProgressionGates;
import za.co.neroland.nerolandcore.sideconfig.Channel;
import za.co.neroland.nerolandcore.sideconfig.SideConfig;
import za.co.neroland.nerolandcore.sideconfig.SideMode;
import za.co.neroland.nerolandcore.sideconfig.SlotGroup;

/**
 * Automated crop tower: a controller with a casing column that farms virtual crop slots on a bounded,
 * cursor-batched cycle. Planting, growth cost, gate, capped yield, genetics and fertiliser all reuse the
 * SAME shared rules as ordinary crops, so a tower can never out-produce an equivalent farm or duplicate. The
 * structure is validated on a slow interval only — never scanned per tick.
 */
public final class CropTowerControllerBlockEntity extends AbstractMachineBlockEntity implements WorldlyContainer {
    private static final int SEED_START = 0;
    private static final int SEED_END = 2;
    private static final int FERTILISER = 3;
    private static final int OUTPUT_START = 4;
    private static final int SLOT_COUNT = 10;
    private static final int[] SLOTS = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private final FluidBuffer nutrient;
    private final TowerSlot[] slots;
    private int height;
    private boolean formed;
    private int cursor;
    private int workTimer;
    private int revalidateTimer;

    public CropTowerControllerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CROP_TOWER_CONTROLLER.get(), pos, state, AgricultureConfig.MACHINE_ENERGY_CAPACITY.get(),
                AgricultureConfig.MACHINE_ENERGY_RATE.get(), 0, stack -> null);
        this.nutrient = new FluidBuffer(AgricultureConfig.MACHINE_FLUID_CAPACITY.get(), this::setChanged);
        int capacity = TowerShape.capacity(AgricultureConfig.TOWER_MAX_HEIGHT.get(), AgricultureConfig.TOWER_SLOTS_PER_LAYER.get());
        this.slots = new TowerSlot[Math.max(1, capacity)];
        for (int i = 0; i < slots.length; i++) slots[i] = new TowerSlot();
        installSideConfig(SideConfig.builder()
                .channel(Channel.ITEM, SlotGroup.of("input", SEED_START, SEED_START + 1, SEED_END, FERTILISER),
                        SlotGroup.of("output", OUTPUT_START, 5, 6, 7, 8, 9))
                .channel(Channel.FLUID).channel(Channel.ENERGY)
                .allow(Channel.ENERGY, SideMode.OUTPUT, false).build())
                .withItems(() -> this).withFluid(this::getFluid);
        this.workTimer = 1 + AreaWork.phaseOffset(pos, AgricultureConfig.AUTOMATION_INTERVAL.get());
    }

    public NeroFluidStorage getFluid() { return nutrient; }
    public boolean formed() { return formed; }
    public int height() { return height; }

    public int activeSlots() {
        return Math.min(slots.length, TowerShape.slots(height, AgricultureConfig.TOWER_MAX_HEIGHT.get(),
                AgricultureConfig.TOWER_SLOTS_PER_LAYER.get()));
    }

    public static void tick(Level level, BlockPos pos, BlockState state, CropTowerControllerBlockEntity tower) {
        AbstractMachineBlockEntity.tick(level, pos, state, tower);
        if (!(level instanceof ServerLevel serverLevel)) return;
        if (--tower.revalidateTimer <= 0) {
            tower.revalidateTimer = AgricultureConfig.TOWER_REVALIDATE_TICKS.get();
            tower.revalidate(serverLevel);
        }
        if (--tower.workTimer <= 0) {
            tower.workTimer = Math.max(1, AgricultureConfig.AUTOMATION_INTERVAL.get());
            if (tower.formed) tower.runCycle(serverLevel);
        }
    }

    private void revalidate(ServerLevel level) {
        int max = AgricultureConfig.TOWER_MAX_HEIGHT.get();
        int count = 0;
        for (int y = 1; y <= max; y++) {
            if (level.getBlockState(worldPosition.above(y)).is(ModBlocks.CROP_TOWER_FRAME.get())) count++;
            else break;
        }
        height = count;
        boolean nowFormed = TowerShape.valid(height, AgricultureConfig.TOWER_MIN_HEIGHT.get(), max);
        if (nowFormed != formed) {
            formed = nowFormed;
            setChanged();
        }
    }

    private void runCycle(ServerLevel level) {
        int active = activeSlots();
        if (active <= 0) return;
        int perPass = AgricultureConfig.TOWER_SLOTS_PER_PASS.get();
        ServerPlayer player = nearbyPlayer(level);
        for (int i = 0; i < perPass; i++) {
            int index = Math.floorMod(cursor + i, active);
            TowerSlot slot = slots[index];
            if (slot.isEmpty()) {
                tryPlant(level, slot);
            } else if (slot.mature()) {
                tryHarvest(level, slot, player);
            } else {
                tryGrow(level, slot, player);
            }
        }
        cursor = Math.floorMod(cursor + perPass, active);
        setChanged();
    }

    private void tryPlant(ServerLevel level, TowerSlot slot) {
        for (int s = SEED_START; s <= SEED_END; s++) {
            ItemStack seed = items.get(s);
            if (!seed.is(ModItems.RESOURCE_SEED.get())) continue;
            MaterialVariant variant = seed.get(ModDataComponents.MATERIAL_VARIANT.get());
            if (variant == null) continue;
            var lookup = MaterialCatalog.forServer(level.getServer()).lookup(variant.material());
            if (!lookup.permitsGrowth()) continue;
            var definition = lookup.material().orElseThrow().definition();
            Genetics genetics = seed.getOrDefault(ModDataComponents.GENETICS.get(), Genetics.EMPTY);
            slot.plant(definition.id(), definition.tier(), seed.getOrDefault(ModDataComponents.HARVEST_COUNT.get(), 0), genetics);
            seed.shrink(1);
            return;
        }
    }

    private void tryGrow(ServerLevel level, TowerSlot slot, @Nullable ServerPlayer player) {
        var lookup = MaterialCatalog.forServer(level.getServer()).lookup(slot.material());
        if (!lookup.permitsGrowth()) return;
        var definition = lookup.material().orElseThrow().definition();
        if (definition.gate() != null && (player == null || !ProgressionGates.isOpen(player, definition.gate()))) return;
        int energyCost = AgricultureConfig.GROW_BED_ENERGY_COST.get();
        int nutrientCost = AgricultureConfig.GROW_BED_NUTRIENT_COST.get();
        if (energyCost > 0 && getEnergy().extract(energyCost, true) < energyCost) return;
        if (nutrientCost > 0 && (nutrient.getFluid() != ModFluids.NUTRIENT.get() || nutrient.drain(nutrientCost, true) < nutrientCost)) return;
        if (energyCost > 0) getEnergy().extract(energyCost, false);
        if (nutrientCost > 0) nutrient.drain(nutrientCost, false);
        slot.grow(GeneticEffects.growthStep(hasSpeedFertiliser() ? 2 : 1, slot.genetics()));
    }

    private void tryHarvest(ServerLevel level, TowerSlot slot, @Nullable ServerPlayer player) {
        var lookup = MaterialCatalog.forServer(level.getServer()).lookup(slot.material());
        if (!lookup.permitsGrowth()) return;
        var definition = lookup.material().orElseThrow().definition();
        if (definition.gate() != null && (player == null || !ProgressionGates.isOpen(player, definition.gate()))) return;
        int cap = TierBalance.yieldCap(definition.tier(), AgricultureConfig.YIELD_TIER_CAP_BASE.get(),
                AgricultureConfig.YIELD_TIER_CAP_STEP.get());
        double cycleYield = za.co.neroland.neroagriculture.cycle.Cycles.current(level.getServer(),
                level.dimension().identifier(), level.getGameTime()).yield();
        int amount = (int) Math.floor(YieldCurve.scaledCapped(definition.yield(), slot.harvestCount(),
                AgricultureConfig.YIELD_MULTIPLIER.get(), cap) * cycleYield) + GeneticEffects.yieldBonus(slot.genetics())
                + (consumeYieldFertiliser() ? AgricultureConfig.FERTILISER_MAX_DOSE.get() : 0);
        ItemStack essence = new ItemStack(ModItems.MATERIAL_ESSENCE.get(), Math.min(64, Math.max(0, amount)));
        essence.set(ModDataComponents.MATERIAL_VARIANT.get(), MaterialVariant.of(slot.material(), slot.family()));
        if (amount <= 0 || !insertOutput(essence)) return;
        slot.harvested();
    }

    private boolean hasSpeedFertiliser() {
        return items.get(FERTILISER).is(ModItems.SPEED_FERTILISER.get());
    }

    private boolean consumeYieldFertiliser() {
        if (!items.get(FERTILISER).is(ModItems.YIELD_FERTILISER.get())) return false;
        items.get(FERTILISER).shrink(1);
        return true;
    }

    private boolean insertOutput(ItemStack stack) {
        for (int s = OUTPUT_START; s < SLOT_COUNT && !stack.isEmpty(); s++) {
            ItemStack existing = items.get(s);
            if (existing.isEmpty()) {
                items.set(s, stack.copy());
                return true;
            }
            if (ItemStack.isSameItemSameComponents(existing, stack)
                    && existing.getCount() + stack.getCount() <= existing.getMaxStackSize()) {
                existing.grow(stack.getCount());
                return true;
            }
        }
        return false;
    }

    @Nullable
    private ServerPlayer nearbyPlayer(ServerLevel level) {
        Player player = level.getNearestPlayer(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5,
                worldPosition.getZ() + 0.5, 24.0, false);
        return player instanceof ServerPlayer serverPlayer ? serverPlayer : null;
    }

    public String status() {
        int planted = 0;
        for (TowerSlot slot : slots) if (!slot.isEmpty()) planted++;
        return (formed ? "formed" : "unformed") + " height=" + height + " slots=" + activeSlots()
                + " planted=" + planted + " NF=" + getEnergy().getAmount() + " nutrient=" + nutrient.getAmount() + "mb";
    }

    /** Seeds for planted slots plus stored items are returned when the controller is broken. */
    public void dropTowerContents(Level level) {
        net.minecraft.world.Containers.dropContents(level, worldPosition, this);
        for (TowerSlot slot : slots) {
            if (slot.isEmpty()) continue;
            ItemStack seed = new ItemStack(ModItems.RESOURCE_SEED.get());
            seed.set(ModDataComponents.MATERIAL_VARIANT.get(), MaterialVariant.of(slot.material(), slot.family()));
            seed.set(ModDataComponents.HARVEST_COUNT.get(), slot.harvestCount());
            if (!slot.genetics().isEmpty()) seed.set(ModDataComponents.GENETICS.get(), slot.genetics());
            net.minecraft.world.level.block.Block.popResource(level, worldPosition, seed);
            slot.clear();
        }
    }

    // --- container ---------------------------------------------------------
    @Override public int getContainerSize() { return SLOT_COUNT; }
    @Override public boolean isEmpty() { return items.stream().allMatch(ItemStack::isEmpty); }
    @Override public ItemStack getItem(int slot) { return items.get(slot); }
    @Override public ItemStack removeItem(int slot, int amount) {
        ItemStack out = ContainerHelper.removeItem(items, slot, amount);
        if (!out.isEmpty()) setChanged();
        return out;
    }
    @Override public ItemStack removeItemNoUpdate(int slot) { return ContainerHelper.takeItem(items, slot); }
    @Override public void setItem(int slot, ItemStack stack) { items.set(slot, stack); setChanged(); }
    @Override public boolean stillValid(Player player) {
        return !isRemoved() && player.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5,
                worldPosition.getZ() + 0.5) <= 64.0;
    }
    @Override public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot >= SEED_START && slot <= SEED_END) return stack.is(ModItems.RESOURCE_SEED.get());
        if (slot == FERTILISER) return stack.is(ModItems.SPEED_FERTILISER.get()) || stack.is(ModItems.YIELD_FERTILISER.get());
        return false;
    }
    @Override public int[] getSlotsForFace(Direction side) { return SLOTS.clone(); }
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) { return canPlaceItem(slot, stack); }
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) { return slot >= OUTPUT_START; }
    @Override public void clearContent() { items.clear(); setChanged(); }

    @Override protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, items);
        output.putString("NutrientFluid", BuiltInRegistries.FLUID.getKey(nutrient.getRawFluid()).toString());
        output.putInt("NutrientAmount", nutrient.getRawAmount());
        output.putInt("Height", height);
        output.putInt("Cursor", cursor);
        for (int i = 0; i < slots.length; i++) {
            TowerSlot slot = slots[i];
            if (slot.isEmpty()) continue;
            output.putString("S" + i + "M", slot.material().toString());
            output.putString("S" + i + "F", slot.family().name());
            output.putInt("S" + i + "A", slot.age());
            output.putInt("S" + i + "H", slot.harvestCount());
            output.putInt("S" + i + "Gy", slot.genetics().yield());
            output.putInt("S" + i + "Gs", slot.genetics().speed());
            output.putInt("S" + i + "Gh", slot.genetics().hardiness());
            output.putInt("S" + i + "Go", slot.genetics().oxygenOutput());
            output.putInt("S" + i + "Gf", slot.genetics().foodPotency());
        }
    }

    @Override protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        ContainerHelper.loadAllItems(input, items);
        Fluid fluid = BuiltInRegistries.FLUID.getValue(Identifier.parse(input.getStringOr("NutrientFluid", "minecraft:empty")));
        nutrient.setRaw(fluid == null ? Fluids.EMPTY : fluid, input.getIntOr("NutrientAmount", 0));
        height = Math.max(0, input.getIntOr("Height", 0));
        cursor = Math.max(0, input.getIntOr("Cursor", 0));
        formed = TowerShape.valid(height, AgricultureConfig.TOWER_MIN_HEIGHT.get(), AgricultureConfig.TOWER_MAX_HEIGHT.get());
        for (int i = 0; i < slots.length; i++) {
            String material = input.getStringOr("S" + i + "M", "");
            if (material.isBlank()) {
                slots[i].clear();
                continue;
            }
            try {
                Genetics genetics = new Genetics(input.getIntOr("S" + i + "Gy", 0), input.getIntOr("S" + i + "Gs", 0),
                        input.getIntOr("S" + i + "Gh", 0), input.getIntOr("S" + i + "Go", 0), input.getIntOr("S" + i + "Gf", 0));
                slots[i].set(Identifier.parse(material), EssenceFamily.valueOf(input.getStringOr("S" + i + "F", "TERRAN")),
                        input.getIntOr("S" + i + "A", 0), input.getIntOr("S" + i + "H", 0), genetics);
            } catch (RuntimeException e) {
                slots[i].clear();
            }
        }
    }
}
