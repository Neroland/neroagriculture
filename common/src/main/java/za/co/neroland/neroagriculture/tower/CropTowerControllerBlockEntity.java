package za.co.neroland.neroagriculture.tower;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
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
import za.co.neroland.neroagriculture.content.FragmentTier;
import za.co.neroland.neroagriculture.content.MaterialVariant;
import za.co.neroland.neroagriculture.crop.GrowthRules;
import za.co.neroland.neroagriculture.crop.YieldCurve;
import za.co.neroland.neroagriculture.fluid.ModFluids;
import za.co.neroland.neroagriculture.genetics.GeneticEffects;
import za.co.neroland.neroagriculture.genetics.Genetics;
import za.co.neroland.neroagriculture.machine.SideConfigMigration;
import za.co.neroland.neroagriculture.menu.CropTowerMenu;
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
import za.co.neroland.nerolandcore.sideconfig.SidePreset;
import za.co.neroland.nerolandcore.sideconfig.SlotGroup;

/**
 * Automated crop tower: a controller with a casing column that farms virtual crop slots on a bounded,
 * cursor-batched cycle. Planting, growth cost, gate, capped yield, genetics and fertiliser all reuse the
 * SAME shared rules as ordinary crops, so a tower can never out-produce an equivalent farm or duplicate. The
 * structure is validated on a slow interval only — never scanned per tick.
 */
public final class CropTowerControllerBlockEntity extends AbstractMachineBlockEntity implements WorldlyContainer, MenuProvider {
    private static final int SEED_START = 0;
    private static final int SEED_END = 2;
    private static final int FERTILISER = 3;
    private static final int OUTPUT_START = 4;
    private static final int SLOT_COUNT = 10;
    private static final int[] SLOTS = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
    /** Half a second between blocked-reason recomputes; the status line does not need per-tick precision. */
    private static final int REASON_REFRESH_TICKS = 10;

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private final FluidBuffer nutrient;
    private final TowerSlot[] slots;
    private int height;
    private boolean formed;
    private int cursor;
    private int workTimer;
    private int revalidateTimer;
    private int blockedReason = CropTowerMenu.NO_CROP;
    private long blockedReasonTick = Long.MIN_VALUE;

    // Gauge slots ship as permille fractions: ContainerData syncs shorts, and the configured energy
    // (up to 10,000,000 NF) and fluid (up to 1,000,000 mB) capacities exceed 32,767 (see menu.GaugeData).
    private final ContainerData menuData = new ContainerData() {
        @Override public int get(int index) {
            return switch (index) {
                case 0 -> height;
                case 1 -> activeSlots();
                case 2 -> za.co.neroland.neroagriculture.menu.GaugeData.permille(
                        getEnergy().getAmount(), getEnergy().getCapacity());
                case 3 -> za.co.neroland.neroagriculture.menu.GaugeData.permille(
                        nutrient.getAmount(), nutrient.getCapacity());
                case 4 -> blockedReasonOrdinal();
                default -> 0;
            };
        }
        @Override public void set(int index, int value) { }
        @Override public int getCount() { return CropTowerMenu.DATA_COUNT; }
    };

    /**
     * Aggregate blocker the tower reports to its screen, cached because {@link ContainerData} is polled
     * every tick per open menu. The worst blocker across the slots the cycle actually works decides, so a
     * single stalled slot is never hidden behind a healthy one. Nothing is computed unless a menu is open.
     */
    private int blockedReasonOrdinal() {
        if (!(level instanceof ServerLevel serverLevel)) return blockedReason;
        long now = serverLevel.getGameTime();
        if (blockedReasonTick != Long.MIN_VALUE && now - blockedReasonTick < REASON_REFRESH_TICKS) {
            return blockedReason;
        }
        blockedReasonTick = now;
        blockedReason = aggregateReason(serverLevel);
        return blockedReason;
    }

    /**
     * Mirrors {@link #runCycle} rather than any single one of its branches.
     *
     * <p>Three things the cycle does that a naive "first non-empty slot" read got wrong: an unformed tower
     * returns from {@link #tick} before any slot is touched; only slots below {@link #activeSlots()} are
     * ever worked; and a mature slot goes to {@link #tryHarvest}, whose real blocker is a full output
     * cluster, not the NF/nutrient draw {@link #tryGrow} makes. Each worked, planted slot is therefore
     * evaluated through the branch it would actually take and the results folded with
     * {@link GrowthRules#worst}.</p>
     */
    private int aggregateReason(ServerLevel level) {
        if (!formed) return GrowthRules.BlockedReason.NOT_FORMED.ordinal();
        int active = activeSlots();
        var catalog = MaterialCatalog.forServer(level.getServer());
        ServerPlayer player = nearbyPlayer(level);
        int energyCost = AgricultureConfig.GROW_BED_ENERGY_COST.get();
        int nutrientCost = AgricultureConfig.GROW_BED_NUTRIENT_COST.get();
        boolean hasPower = energyCost <= 0 || getEnergy().extract(energyCost, true) >= energyCost;
        boolean hasNutrient = nutrientCost <= 0
                || nutrient.getFluid() == ModFluids.NUTRIENT.get() && nutrient.drain(nutrientCost, true) >= nutrientCost;

        GrowthRules.BlockedReason aggregate = null;
        for (int i = 0; i < active && i < slots.length; i++) {
            TowerSlot slot = slots[i];
            if (slot.isEmpty()) continue;
            var lookup = catalog.lookup(slot.material());
            var definition = lookup.material().map(material -> material.definition()).orElse(null);
            boolean gate = gateOpen(definition, player);
            GrowthRules.BlockedReason reason = slot.mature()
                    ? GrowthRules.evaluateTowerHarvest(lookup.status(), gate, outputHasRoomFor(slot))
                    : GrowthRules.evaluateTower(lookup.status(), gate, hasPower, hasNutrient);
            aggregate = aggregate == null ? reason : GrowthRules.worst(aggregate, reason);
        }
        return aggregate == null ? CropTowerMenu.NO_CROP : aggregate.ordinal();
    }

    /** The gate half of {@link #tryGrow}/{@link #tryHarvest}, stated once so the status line cannot drift. */
    private boolean gateOpen(@Nullable za.co.neroland.neroagriculture.catalog.MaterialDefinition definition,
            @Nullable ServerPlayer player) {
        if (definition == null) return true;
        return (definition.gate() == null || player != null && ProgressionGates.isOpen(player, definition.gate()))
                && (player == null
                || za.co.neroland.neroagriculture.progression.SiblingOverlays.tierSatisfied(player, definition.tier()));
    }

    /**
     * Whether the output cluster could take at least one fragment of this slot's material — the cheap
     * status-line approximation of {@link #tryHarvest}'s all-or-nothing capacity check (the real harvest
     * needs room for the whole yield, so a nearly-full cluster can read OK for a short while before it
     * blocks). A probe carrying the same variant and tint components keeps the stack-merge test honest.
     */
    private boolean outputHasRoomFor(TowerSlot slot) {
        ItemStack probe = new ItemStack(ModItems.RESOURCE_FRAGMENT.get(), 1);
        probe.set(ModDataComponents.MATERIAL_VARIANT.get(), MaterialVariant.of(slot.material(), slot.family()));
        za.co.neroland.neroagriculture.content.MaterialTints.apply(probe, slot.material());
        return outputCapacityFor(probe) > 0;
    }

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
                .defaultPreset(SidePreset.PROCESSOR)
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
        SideConfigMigration.tick(tower);
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
                tryGrow(level, slot, player, index);
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

    private void tryGrow(ServerLevel level, TowerSlot slot, @Nullable ServerPlayer player, int slotIndex) {
        var lookup = MaterialCatalog.forServer(level.getServer()).lookup(slot.material());
        if (!lookup.permitsGrowth()) return;
        var definition = lookup.material().orElseThrow().definition();
        if ((definition.gate() != null && (player == null || !ProgressionGates.isOpen(player, definition.gate())))
                || (player != null && !za.co.neroland.neroagriculture.progression.SiblingOverlays.tierSatisfied(player, definition.tier()))) return;
        int energyCost = AgricultureConfig.GROW_BED_ENERGY_COST.get() * environmentCostMultiplier(level, slot, definition.tier(), slotIndex);
        int nutrientCost = AgricultureConfig.GROW_BED_NUTRIENT_COST.get();
        if (energyCost > 0 && getEnergy().extract(energyCost, true) < energyCost) return;
        if (nutrientCost > 0 && (nutrient.getFluid() != ModFluids.NUTRIENT.get() || nutrient.drain(nutrientCost, true) < nutrientCost)) return;
        if (energyCost > 0) getEnergy().extract(energyCost, false);
        if (nutrientCost > 0) nutrient.drain(nutrientCost, false);
        slot.grow(GeneticEffects.growthStep(hasSpeedFertiliser() ? 2 : 1, slot.genetics()));
    }

    /**
     * Per-layer environment check reusing the same climate evaluation as the grow-bed path
     * ({@link za.co.neroland.neroagriculture.environment.CropClimate}). Where a grow bed would refuse to
     * grow the crop (hostile world, or a high tier outside a sealed greenhouse), the tower keeps working
     * but pays the configured NF surcharge ({@code crop_tower.hostile_environment_nf_multiplier}) — an
     * engineered environment inside the casing, priced in power instead of a hard stop.
     */
    private int environmentCostMultiplier(ServerLevel level, TowerSlot slot, FragmentTier tier, int slotIndex) {
        int slotsPerLayer = Math.max(1, AgricultureConfig.TOWER_SLOTS_PER_LAYER.get());
        BlockPos layerPos = worldPosition.above(1 + slotIndex / slotsPerLayer);
        var climate = za.co.neroland.neroagriculture.environment.CropClimate.evaluate(
                za.co.neroland.neroagriculture.environment.GrowthEnvironment.worldProfile(level, layerPos),
                za.co.neroland.neroagriculture.greenhouse.GreenhouseIndex.sealedAt(level, layerPos),
                tier.ordinal(),
                za.co.neroland.neroagriculture.environment.CropClimate.thresholdOrdinal(AgricultureConfig.CONTROLLED_TIER.get()),
                slot.genetics().hardiness(), AgricultureConfig.GENETICS_HARDINESS_RELAX.get());
        return climate == za.co.neroland.neroagriculture.environment.CropClimate.Result.OK ? 1
                : Math.max(1, AgricultureConfig.TOWER_HOSTILE_NF_MULTIPLIER.get());
    }

    /**
     * All-or-nothing harvest: the full yield must fit across the output cluster (topping up matching
     * stacks and splitting into as many stacks as needed — nothing beyond one stack is ever discarded)
     * before anything is consumed. Fertiliser is charged only after the insert succeeds, so a jammed
     * output never burns fertiliser, and a zero-yield harvest still resets the slot so the cycle never
     * spins on a mature, yieldless crop.
     */
    private void tryHarvest(ServerLevel level, TowerSlot slot, @Nullable ServerPlayer player) {
        var lookup = MaterialCatalog.forServer(level.getServer()).lookup(slot.material());
        if (!lookup.permitsGrowth()) return;
        var definition = lookup.material().orElseThrow().definition();
        if ((definition.gate() != null && (player == null || !ProgressionGates.isOpen(player, definition.gate())))
                || (player != null && !za.co.neroland.neroagriculture.progression.SiblingOverlays.tierSatisfied(player, definition.tier()))) return;
        int cap = TierBalance.yieldCap(definition.tier(), AgricultureConfig.YIELD_TIER_CAP_BASE.get(),
                AgricultureConfig.YIELD_TIER_CAP_STEP.get());
        double cycleYield = za.co.neroland.neroagriculture.cycle.Cycles.current(level.getServer(),
                level.dimension().identifier(), level.getGameTime()).yield();
        boolean fertilised = items.get(FERTILISER).is(ModItems.YIELD_FERTILISER.get());
        int amount = (int) Math.floor(YieldCurve.scaledCapped(definition.yield(), slot.harvestCount(),
                AgricultureConfig.YIELD_MULTIPLIER.get(), cap) * cycleYield) + GeneticEffects.yieldBonus(slot.genetics())
                + (fertilised ? AgricultureConfig.FERTILISER_MAX_DOSE.get() : 0);
        if (amount <= 0) {
            slot.harvested();
            return;
        }
        ItemStack probe = new ItemStack(ModItems.RESOURCE_FRAGMENT.get());
        probe.set(ModDataComponents.MATERIAL_VARIANT.get(), MaterialVariant.of(slot.material(), slot.family()));
        za.co.neroland.neroagriculture.content.MaterialTints.apply(probe, slot.material());
        if (outputCapacityFor(probe) < amount) return; // output jammed: keep the slot mature, consume nothing
        insertAmount(probe, amount);
        if (fertilised) items.get(FERTILISER).shrink(1);
        slot.harvested();
    }

    private boolean hasSpeedFertiliser() {
        return items.get(FERTILISER).is(ModItems.SPEED_FERTILISER.get());
    }

    /** Total count of {@code probe}-equivalent items the output cluster can still absorb (empties + top-ups). */
    private int outputCapacityFor(ItemStack probe) {
        int room = 0;
        for (int s = OUTPUT_START; s < SLOT_COUNT; s++) {
            ItemStack existing = items.get(s);
            if (existing.isEmpty()) room += probe.getMaxStackSize();
            else if (ItemStack.isSameItemSameComponents(existing, probe)) {
                room += Math.max(0, existing.getMaxStackSize() - existing.getCount());
            }
        }
        return room;
    }

    /** Distribute {@code amount} copies of {@code probe} across the cluster; caller pre-checks capacity. */
    private void insertAmount(ItemStack probe, int amount) {
        int remaining = amount;
        for (int s = OUTPUT_START; s < SLOT_COUNT && remaining > 0; s++) {
            ItemStack existing = items.get(s);
            if (existing.isEmpty()) {
                int take = Math.min(probe.getMaxStackSize(), remaining);
                items.set(s, probe.copyWithCount(take));
                remaining -= take;
            } else if (ItemStack.isSameItemSameComponents(existing, probe)) {
                int take = Math.min(Math.max(0, existing.getMaxStackSize() - existing.getCount()), remaining);
                existing.grow(take);
                remaining -= take;
            }
        }
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

    /**
     * Seeds for planted slots plus stored items are returned whenever the controller is removed —
     * survival or creative break, explosion, or replacement — via {@link #preRemoveSideEffects}.
     */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (this.level instanceof ServerLevel) dropTowerContents(this.level);
    }

    /** Seeds for planted slots plus stored items are returned when the controller is broken. */
    public void dropTowerContents(Level level) {
        net.minecraft.world.Containers.dropContents(level, worldPosition, this);
        for (TowerSlot slot : slots) {
            if (slot.isEmpty()) continue;
            ItemStack seed = new ItemStack(ModItems.RESOURCE_SEED.get());
            seed.set(ModDataComponents.MATERIAL_VARIANT.get(), MaterialVariant.of(slot.material(), slot.family()));
            za.co.neroland.neroagriculture.content.MaterialTints.apply(seed, slot.material());
            seed.set(ModDataComponents.HARVEST_COUNT.get(), slot.harvestCount());
            if (!slot.genetics().isEmpty()) seed.set(ModDataComponents.GENETICS.get(), slot.genetics());
            net.minecraft.world.level.block.Block.popResource(level, worldPosition, seed);
            slot.clear();
        }
    }

    // --- container ---------------------------------------------------------
    @Override public Component getDisplayName() { return getBlockState().getBlock().getName(); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new CropTowerMenu(id, inventory, this, menuData, worldPosition);
    }

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
        SideConfigMigration.save(output);
    }

    @Override protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        SideConfigMigration.load(this, input);
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
                slots[i].set(Identifier.parse(material), FragmentTier.valueOf(input.getStringOr("S" + i + "F", "TERRITE")),
                        input.getIntOr("S" + i + "A", 0), input.getIntOr("S" + i + "H", 0), genetics);
            } catch (RuntimeException e) {
                slots[i].clear();
            }
        }
    }
}
