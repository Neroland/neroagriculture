package za.co.neroland.neroagriculture.machine;

import java.util.Comparator;
import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
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
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.neroagriculture.catalog.MaterialCatalog;
import za.co.neroland.neroagriculture.catalog.ResolvedMaterial;
import za.co.neroland.neroagriculture.config.AgricultureConfig;
import za.co.neroland.neroagriculture.content.FragmentCharge;
import za.co.neroland.neroagriculture.content.AgricultureUpgradeItem;
import za.co.neroland.neroagriculture.content.FragmentTier;
import za.co.neroland.neroagriculture.content.MaterialVariant;
import za.co.neroland.neroagriculture.menu.FoundationMachineMenu;
import za.co.neroland.neroagriculture.network.AgricultureNetwork;
import za.co.neroland.neroagriculture.network.MachineMenuPositionPayload;
import za.co.neroland.neroagriculture.recipe.FabricationRecipe;
import za.co.neroland.neroagriculture.registry.ModBlockEntities;
import za.co.neroland.neroagriculture.registry.ModDataComponents;
import za.co.neroland.neroagriculture.registry.ModItems;
import za.co.neroland.neroagriculture.registry.ModRecipeSerializers;
import za.co.neroland.nerolandcore.fluid.FluidBuffer;
import za.co.neroland.nerolandcore.fluid.NeroFluidStorage;
import za.co.neroland.nerolandcore.machine.AbstractMachineBlockEntity;
import za.co.neroland.nerolandcore.progression.MaterialMilestoneDefinitions;
import za.co.neroland.nerolandcore.progression.MaterialMilestones;
import za.co.neroland.nerolandcore.progression.MaterialObservation;
import za.co.neroland.nerolandcore.progression.ProgressionGates;
import za.co.neroland.nerolandcore.sideconfig.Channel;
import za.co.neroland.nerolandcore.sideconfig.SideConfig;
import za.co.neroland.nerolandcore.sideconfig.SideMode;
import za.co.neroland.nerolandcore.sideconfig.SlotGroup;

/** Persistent, automatable Stage 5 fabrication machine shared by the finite machine ids. */
public final class FoundationMachineBlockEntity extends AbstractMachineBlockEntity
        implements WorldlyContainer, MenuProvider {
    public static final int SLOT_COUNT = 5;
    public static final int PRIMARY = 0;
    public static final int SECONDARY = 1;
    public static final int TERTIARY = 2;
    public static final int OUTPUT = 3;
    public static final int SECONDARY_OUTPUT = 4;
    public static final int UPGRADE_START = 5;
    public static final int TOTAL_MENU_SLOTS = 7;
    private static final int[] SLOTS = {0, 1, 2, 3, 4};
    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private final FluidBuffer fluid;
    private int fabricationProgress;
    private int fabricationMax;
    private MachineBlockedReason blockedReason = MachineBlockedReason.IDLE;
    private String activeRecipe = "";

    private final ContainerData menuData = new ContainerData() {
        @Override public int get(int index) {
            return switch (index) {
                case 0 -> fabricationProgress;
                case 1 -> fabricationMax;
                case 2 -> (int) Math.min(Integer.MAX_VALUE, getEnergy().getAmount());
                case 3 -> blockedReason.ordinal();
                case 4 -> kind().ordinal();
                default -> 0;
            };
        }
        @Override public void set(int index, int value) {
            switch (index) {
                case 0 -> fabricationProgress = value;
                case 1 -> fabricationMax = value;
                case 3 -> blockedReason = MachineBlockedReason.byOrdinal(value);
                default -> { }
            }
        }
        @Override public int getCount() { return 5; }
    };

    public FoundationMachineBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FOUNDATION_MACHINE.get(), pos, state,
                AgricultureConfig.MACHINE_ENERGY_CAPACITY.get(), AgricultureConfig.MACHINE_ENERGY_RATE.get(),
                2, AgricultureUpgradeItem.CLASSIFIER);
        this.fluid = new FluidBuffer(AgricultureConfig.MACHINE_FLUID_CAPACITY.get(), this::setChanged);
        installSideConfig(SideConfig.builder()
                .channel(Channel.ITEM, SlotGroup.of("input", PRIMARY, SECONDARY, TERTIARY),
                        SlotGroup.of("output", OUTPUT, SECONDARY_OUTPUT))
                .channel(Channel.FLUID).channel(Channel.ENERGY)
                .allow(Channel.ENERGY, SideMode.OUTPUT, false).build())
                .withItems(() -> this).withFluid(this::getFluid);
    }

    public NeroFluidStorage getFluid() { return fluid; }
    public MachineKind kind() { return MachineKind.of(getBlockState().getBlock()); }
    public MachineBlockedReason blockedReason() { return blockedReason; }
    public int fabricationProgress() { return fabricationProgress; }
    public int fabricationMax() { return fabricationMax; }

    public static void tick(Level level, BlockPos pos, BlockState state, FoundationMachineBlockEntity machine) {
        AbstractMachineBlockEntity.tick(level, pos, state, machine);
        if (level instanceof ServerLevel serverLevel) machine.tickFabrication(serverLevel);
    }

    private void tickFabrication(ServerLevel level) {
        if (kind() == MachineKind.OTHER || kind() == MachineKind.RESEARCH_BENCH) {
            stop(MachineBlockedReason.IDLE);
            return;
        }
        Resolution resolution = resolve(level);
        if (resolution.operation == null) {
            stop(resolution.reason);
            return;
        }
        Operation operation = resolution.operation;
        if (!activeRecipe.equals(operation.key)) {
            fabricationProgress = 0;
            activeRecipe = operation.key;
        }
        var modifiers = modifiers();
        fabricationMax = Math.max(1, (int) Math.round(operation.recipe.ticks() / modifiers.speedMultiplier()));
        int totalEnergy = Math.max(0, (int) Math.round(operation.recipe.energy() * modifiers.energyMultiplier()));
        int energyPerTick = FabricationRules.energyPerTick(totalEnergy, fabricationMax);
        if (getEnergy().extract(energyPerTick, true) < energyPerTick) {
            blockedReason = MachineBlockedReason.NO_POWER;
            return;
        }
        getEnergy().extract(energyPerTick, false);
        fabricationProgress++;
        blockedReason = MachineBlockedReason.RUNNING;
        if (fabricationProgress >= fabricationMax) {
            Resolution finalCheck = resolve(level);
            if (finalCheck.operation == null) {
                stop(finalCheck.reason);
                return;
            }
            apply(finalCheck.operation);
            unlockNextTier(level, finalCheck.operation.output);
            fabricationProgress = 0;
            blockedReason = MachineBlockedReason.COMPLETE;
        }
        setChanged();
    }

    private Resolution resolve(ServerLevel level) {
        return switch (kind()) {
            case EXTRACTOR -> resolveExtraction(level);
            case INFUSER -> resolveInfusion(level);
            case SYNTHESIZER -> items.get(PRIMARY).is(ModItems.RESOURCE_FRAGMENT.get())
                    ? resolveConversion(level) : resolveSynthesis(level);
            default -> new Resolution(MachineBlockedReason.IDLE, null);
        };
    }

    private Resolution resolveExtraction(ServerLevel level) {
        var catalog = MaterialCatalog.forServer(level.getServer());
        Optional<ResolvedMaterial> match = MaterialOperations.matchAny(items.get(PRIMARY), catalog);
        if (match.isEmpty()) return fail(MachineBlockedReason.NO_RECIPE);
        var definition = match.get().definition();
        if (!catalog.lookup(definition.id()).permitsGrowth()) return fail(MachineBlockedReason.CATALOG_DISABLED);
        FabricationRecipe recipe = findRecipe(level, ModRecipeSerializers.EXTRACTION.get(),
                candidate -> candidate.material().isEmpty() || candidate.material().get().equals(definition.id()));
        if (recipe == null) return fail(MachineBlockedReason.NO_RECIPE);
        ItemStack neutral = new ItemStack(MaterialOperations.neutralFragment(definition.tier()),
                recipe.resultTemplate().count());
        ItemStack material = new ItemStack(ModItems.RESOURCE_FRAGMENT.get());
        material.set(ModDataComponents.MATERIAL_VARIANT.get(), MaterialVariant.of(definition.id(), definition.tier()));
        Operation operation = operation(recipe, recipe.inputCount(), 0, 0, neutral, material);
        return canOutput(operation) ? ok(operation) : fail(MachineBlockedReason.OUTPUT_FULL);
    }

    private Resolution resolveInfusion(ServerLevel level) {
        boolean charging = items.get(SECONDARY).is(ModItems.BLANK_SEED.get());
        FabricationRecipe recipe = findRecipe(level, ModRecipeSerializers.INFUSING.get(), candidate ->
                candidate.resultTemplate().create().is(ModItems.CHARGED_SEED.get()) == charging);
        if (recipe == null || recipe.family().isEmpty()) return fail(MachineBlockedReason.NO_RECIPE);
        FragmentTier family = recipe.family().get();
        if (!gateOpen(level, family)) return fail(MachineBlockedReason.GATE_CLOSED);
        ItemStack result = recipe.assemble(new SingleRecipeInput(items.get(PRIMARY)));
        int secondary = 0;
        if (result.is(ModItems.CHARGED_SEED.get())) {
            if (!items.get(SECONDARY).is(ModItems.BLANK_SEED.get())) return fail(MachineBlockedReason.NO_RECIPE);
            result.set(ModDataComponents.FRAGMENT_CHARGE.get(), FragmentCharge.of(family));
            secondary = 1;
        }
        Operation operation = operation(recipe, recipe.inputCount(), secondary, 0, result, ItemStack.EMPTY);
        return canOutput(operation) ? ok(operation) : fail(MachineBlockedReason.OUTPUT_FULL);
    }

    private Resolution resolveSynthesis(ServerLevel level) {
        var catalog = MaterialCatalog.forServer(level.getServer());
        Optional<ResolvedMaterial> match = MaterialOperations.matchAny(items.get(PRIMARY), catalog);
        if (match.isEmpty()) return fail(MachineBlockedReason.NO_RECIPE);
        var definition = match.get().definition();
        if (!catalog.lookup(definition.id()).permitsGrowth()) return fail(MachineBlockedReason.CATALOG_DISABLED);
        FabricationRecipe recipe = findRecipe(level, ModRecipeSerializers.SYNTHESIZING.get(), candidate ->
                (candidate.material().isEmpty() || candidate.material().get().equals(definition.id()))
                        && (candidate.family().isEmpty() || candidate.family().get() == definition.tier()));
        if (recipe == null) return fail(MachineBlockedReason.NO_RECIPE);
        // Resource seed = the real resource (PRIMARY) + N matching Tier Fragments (SECONDARY) + a Prospora
        // Seed base (TERTIARY). Requiring the real resource keeps seeds an amplifier, never a way to obtain
        // a resource the player has never seen; the Tier Fragment ties seed cost to ladder progress.
        int fragmentCost = FabricationRules.fragmentsPerSeed(definition.tier());
        var tierFragment = MaterialOperations.neutralFragment(definition.tier());
        if (!items.get(SECONDARY).is(tierFragment)
                || items.get(SECONDARY).getCount() < fragmentCost
                || !items.get(TERTIARY).is(ModItems.PROSPORA_SEED.get())) {
            return fail(MachineBlockedReason.INVALID_COMPONENT);
        }
        ServerPlayer player = nearbyPlayer(level);
        if (player == null || !gateOpen(player, definition.tier())) return fail(MachineBlockedReason.GATE_CLOSED);
        if (!MaterialMilestones.isObserved(player, MaterialMilestoneDefinitions.MATERIAL_DISCOVERED,
                definition.id())) return fail(MachineBlockedReason.MILESTONE_REQUIRED);
        if (!MaterialMilestones.isObserved(player, MachineProgression.RESOURCE_RESEARCH,
                definition.id())) return fail(MachineBlockedReason.RESEARCH_REQUIRED);
        ItemStack seed = new ItemStack(ModItems.RESOURCE_SEED.get());
        seed.set(ModDataComponents.MATERIAL_VARIANT.get(), MaterialVariant.of(definition.id(), definition.tier()));
        Operation operation = operation(recipe, recipe.inputCount(), fragmentCost, 1, seed, ItemStack.EMPTY);
        return canOutput(operation) ? ok(operation) : fail(MachineBlockedReason.OUTPUT_FULL);
    }

    private Resolution resolveConversion(ServerLevel level) {
        MaterialVariant variant = items.get(PRIMARY).get(ModDataComponents.MATERIAL_VARIANT.get());
        if (variant == null) return fail(MachineBlockedReason.INVALID_COMPONENT);
        var lookup = MaterialCatalog.forServer(level.getServer()).lookup(variant.material());
        if (!lookup.permitsGrowth()) return fail(MachineBlockedReason.CATALOG_DISABLED);
        var definition = lookup.material().orElseThrow().definition();
        if (variant.family() != definition.tier()) return fail(MachineBlockedReason.INVALID_COMPONENT);
        ServerPlayer player = nearbyPlayer(level);
        if (player == null || !gateOpen(player, definition.tier())) return fail(MachineBlockedReason.GATE_CLOSED);
        if (!MaterialMilestones.isObserved(player, MaterialMilestoneDefinitions.MATERIAL_DISCOVERED,
                definition.id())) return fail(MachineBlockedReason.MILESTONE_REQUIRED);
        if (!MaterialMilestones.isObserved(player, MachineProgression.RESOURCE_RESEARCH,
                definition.id())) return fail(MachineBlockedReason.RESEARCH_REQUIRED);
        FabricationRecipe recipe = findRecipe(level, ModRecipeSerializers.CONVERSION.get(), candidate ->
                candidate.material().isPresent() && candidate.material().get().equals(definition.id()));
        if (recipe == null || recipe.inputCount() < definition.conversion()) return fail(MachineBlockedReason.NO_RECIPE);
        ItemStack result = recipe.assemble(new SingleRecipeInput(items.get(PRIMARY)));
        if (!BuiltInRegistries.ITEM.getKey(result.getItem()).equals(definition.output())) {
            return fail(MachineBlockedReason.INVALID_COMPONENT);
        }
        Operation operation = operation(recipe, recipe.inputCount(), 0, 0, result, ItemStack.EMPTY);
        return canOutput(operation) ? ok(operation) : fail(MachineBlockedReason.OUTPUT_FULL);
    }

    public boolean tryResearch(ServerPlayer player) {
        if (kind() != MachineKind.RESEARCH_BENCH || !stillValid(player)) return false;
        ServerLevel level = player.level();
        FabricationRecipe recipe = findRecipe(level, ModRecipeSerializers.RESEARCHING.get(), candidate -> true);
        if (recipe == null || recipe.material().isEmpty()) {
            stop(MachineBlockedReason.NO_RECIPE);
            return false;
        }
        var subject = recipe.material().get();
        ItemStack displayResult = recipe.assemble(new SingleRecipeInput(items.get(PRIMARY)));
        var milestone = displayResult.is(ModItems.FOOD_SEED.get()) ? MachineProgression.FOOD_RESEARCH
                : displayResult.is(ModItems.ALIEN_SEED.get()) ? MachineProgression.ALIEN_RESEARCH
                : MachineProgression.RESOURCE_RESEARCH;
        if (milestone == MachineProgression.RESOURCE_RESEARCH) {
            var lookup = MaterialCatalog.forServer(level.getServer()).lookup(subject);
            if (!lookup.permitsGrowth()) {
                stop(MachineBlockedReason.CATALOG_DISABLED);
                return false;
            }
            var definition = lookup.material().orElseThrow().definition();
            if (!MaterialMilestones.isObserved(player, MaterialMilestoneDefinitions.MATERIAL_DISCOVERED, subject)) {
                if (definition.worldRestriction() != null
                        && definition.worldRestriction().dimension().getNamespace().equals("nerospace")) {
                    stop(MachineBlockedReason.MILESTONE_REQUIRED);
                    return false;
                }
                String sampleNamespace = BuiltInRegistries.ITEM.getKey(items.get(PRIMARY).getItem()).getNamespace();
                MaterialObservation observation = sampleNamespace.equals(definition.output().getNamespace())
                        ? MaterialObservation.OWNER_MOD : MaterialObservation.PLAYER_PICKUP;
                MaterialMilestones.observe(player, MaterialMilestoneDefinitions.MATERIAL_DISCOVERED,
                        subject, observation);
            }
            if (!MaterialMilestones.isObserved(player, MaterialMilestoneDefinitions.MATERIAL_DISCOVERED, subject)) {
                stop(MachineBlockedReason.MILESTONE_REQUIRED);
                return false;
            }
        }
        if (!MaterialMilestones.isObserved(player, milestone, subject)) {
            MaterialMilestones.observe(player, milestone, subject, MaterialObservation.OWNER_MOD);
        }
        if (!MaterialMilestones.isObserved(player, milestone, subject)) {
            stop(MachineBlockedReason.RESEARCH_REQUIRED);
            return false;
        }
        items.get(PRIMARY).shrink(recipe.inputCount());
        if (milestone == MachineProgression.FOOD_RESEARCH || milestone == MachineProgression.ALIEN_RESEARCH) {
            za.co.neroland.neroagriculture.food.FoodCatalog.lookup(level.getServer(), subject).ifPresent(definition -> {
                ItemStack seed = new ItemStack(definition.kind() == za.co.neroland.neroagriculture.food.FoodDefinition.Kind.ALIEN
                        ? ModItems.ALIEN_SEED.get() : ModItems.FOOD_SEED.get());
                seed.set(ModDataComponents.SPECIES_VARIANT.get(),
                        za.co.neroland.neroagriculture.content.SpeciesVariant.of(definition.id()));
                if (!player.getInventory().add(seed)) player.drop(seed, false);
            });
        }
        blockedReason = MachineBlockedReason.COMPLETE;
        setChanged();
        return true;
    }

    @Nullable
    private FabricationRecipe findRecipe(ServerLevel level, RecipeType<FabricationRecipe> type,
            java.util.function.Predicate<FabricationRecipe> extra) {
        SingleRecipeInput input = new SingleRecipeInput(items.get(PRIMARY));
        return level.recipeAccess().getRecipes().stream()
                .sorted(Comparator.comparing(holder -> holder.id().identifier().toString()))
                .map(RecipeHolder::value)
                .filter(recipe -> recipe.getType().equals(type))
                .map(recipe -> (FabricationRecipe) recipe)
                .filter(recipe -> recipe.matches(input, level) && extra.test(recipe))
                .findFirst().orElse(null);
    }

    private boolean gateOpen(ServerLevel level, FragmentTier family) {
        if (MachineProgression.gate(family) == null) return true;
        ServerPlayer player = nearbyPlayer(level);
        return player != null && gateOpen(player, family);
    }

    /**
     * When the machine finishes producing a tier fragment, open the next tier's native gate for the
     * nearby player (respecting the gate's prerequisites via {@link ProgressionGates#tryOpen}). This is
     * the standalone unlock: extracting Territe opens Refinement, condensing Forgite opens Synthesis,
     * and so on up the ladder — no sibling mod required.
     */
    private void unlockNextTier(ServerLevel level, ItemStack output) {
        FragmentTier produced = fragmentTierOf(output);
        if (produced == null) return;
        var gate = za.co.neroland.neroagriculture.progression.AgricultureGates.gateUnlockedByProducing(produced);
        if (gate == null) return;
        ServerPlayer player = nearbyPlayer(level);
        if (player != null) ProgressionGates.tryOpen(player, gate);
    }

    @Nullable
    private static FragmentTier fragmentTierOf(ItemStack stack) {
        if (stack.is(ModItems.TERRITE_FRAGMENT.get())) return FragmentTier.TERRITE;
        if (stack.is(ModItems.FORGITE_FRAGMENT.get())) return FragmentTier.FORGITE;
        if (stack.is(ModItems.ORBITE_FRAGMENT.get())) return FragmentTier.ORBITE;
        if (stack.is(ModItems.COLONITE_FRAGMENT.get())) return FragmentTier.COLONITE;
        if (stack.is(ModItems.VOIDITE_FRAGMENT.get())) return FragmentTier.VOIDITE;
        return null;
    }

    private static boolean gateOpen(ServerPlayer player, FragmentTier family) {
        var gate = MachineProgression.gate(family);
        return gate == null || ProgressionGates.isOpen(player, gate);
    }

    @Nullable
    private ServerPlayer nearbyPlayer(ServerLevel level) {
        Player player = level.getNearestPlayer(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5,
                worldPosition.getZ() + 0.5, 16.0, false);
        return player instanceof ServerPlayer serverPlayer ? serverPlayer : null;
    }

    private boolean canOutput(Operation operation) {
        return canMerge(OUTPUT, operation.output) && canMerge(SECONDARY_OUTPUT, operation.secondaryOutput);
    }

    private boolean canMerge(int slot, ItemStack addition) {
        if (addition.isEmpty()) return true;
        ItemStack existing = items.get(slot);
        return existing.isEmpty() || ItemStack.isSameItemSameComponents(existing, addition)
                && existing.getCount() + addition.getCount() <= existing.getMaxStackSize();
    }

    private void apply(Operation operation) {
        merge(OUTPUT, operation.output);
        merge(SECONDARY_OUTPUT, operation.secondaryOutput);
        items.get(PRIMARY).shrink(operation.consumePrimary);
        items.get(SECONDARY).shrink(operation.consumeSecondary);
        items.get(TERTIARY).shrink(operation.consumeTertiary);
    }

    private void merge(int slot, ItemStack addition) {
        if (addition.isEmpty()) return;
        if (items.get(slot).isEmpty()) items.set(slot, addition.copy());
        else items.get(slot).grow(addition.getCount());
    }

    private static Operation operation(FabricationRecipe recipe, int primary, int secondary, int tertiary,
            ItemStack output, ItemStack secondaryOutput) {
        String key = recipe.getType() + "|" + recipe.material().map(Object::toString).orElse("") + "|"
                + recipe.family().map(Enum::name).orElse("") + "|"
                + BuiltInRegistries.ITEM.getKey(recipe.resultTemplate().item().value()) + "|"
                + recipe.resultTemplate().count() + "|" + recipe.inputCount() + "|" + recipe.energy()
                + "|" + recipe.ticks();
        return new Operation(key, recipe, primary, secondary, tertiary, output, secondaryOutput);
    }

    private static Resolution ok(Operation operation) { return new Resolution(MachineBlockedReason.RUNNING, operation); }
    private static Resolution fail(MachineBlockedReason reason) { return new Resolution(reason, null); }
    private void stop(MachineBlockedReason reason) {
        if (fabricationProgress != 0 || fabricationMax != 0 || blockedReason != reason) {
            fabricationProgress = 0;
            fabricationMax = 0;
            activeRecipe = "";
            blockedReason = reason;
            setChanged();
        }
    }

    @Override protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, items);
        output.putInt("FluidAmount", fluid.getRawAmount());
        output.putString("Fluid", BuiltInRegistries.FLUID.getKey(fluid.getRawFluid()).toString());
        output.putInt("FabricationProgress", fabricationProgress);
        output.putInt("FabricationMax", fabricationMax);
        output.putInt("BlockedReason", blockedReason.ordinal());
        output.putString("ActiveRecipe", activeRecipe);
    }

    @Override protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        ContainerHelper.loadAllItems(input, items);
        fluid.setRaw(BuiltInRegistries.FLUID.getValue(
                net.minecraft.resources.Identifier.parse(input.getStringOr("Fluid", "minecraft:empty"))),
                input.getIntOr("FluidAmount", 0));
        fabricationProgress = Math.max(0, input.getIntOr("FabricationProgress", 0));
        fabricationMax = Math.max(0, input.getIntOr("FabricationMax", 0));
        blockedReason = MachineBlockedReason.byOrdinal(input.getIntOr("BlockedReason", 0));
        activeRecipe = input.getStringOr("ActiveRecipe", "");
    }

    @Override public Component getDisplayName() { return getBlockState().getBlock().getName(); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            AgricultureNetwork.sendToPlayer(serverPlayer, new MachineMenuPositionPayload(id, worldPosition.asLong()));
        }
        return new FoundationMachineMenu(id, inventory, this, menuData, worldPosition);
    }
    @Override public int[] getSlotsForFace(net.minecraft.core.Direction side) { return SLOTS.clone(); }
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack,
            @Nullable net.minecraft.core.Direction side) { return slot <= TERTIARY && canPlaceItem(slot, stack); }
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack,
            net.minecraft.core.Direction side) { return slot >= OUTPUT; }
    @Override public int getContainerSize() { return TOTAL_MENU_SLOTS; }
    @Override public boolean isEmpty() {
        return items.stream().allMatch(ItemStack::isEmpty)
                && upgrades.items().stream().allMatch(ItemStack::isEmpty);
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
        ItemStack bounded = stack.copyWithCount(Math.min(stack.getCount(), getMaxStackSize(stack)));
        if (slot < SLOT_COUNT) items.set(slot, bounded);
        else upgrades.setStack(slot - UPGRADE_START, bounded);
        setChanged();
    }
    @Override public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot >= UPGRADE_START) return upgrades.isModule(stack);
        if (slot >= OUTPUT) return false;
        return switch (kind()) {
            case EXTRACTOR, RESEARCH_BENCH -> slot == PRIMARY;
            case INFUSER -> slot == PRIMARY || slot == SECONDARY && stack.is(ModItems.BLANK_SEED.get());
            case SYNTHESIZER -> slot == PRIMARY || slot == SECONDARY && stack.is(ModItems.RESOURCE_FRAGMENT.get())
                    || slot == TERTIARY && stack.is(ModItems.CHARGED_SEED.get());
            default -> slot <= TERTIARY;
        };
    }
    @Override public boolean stillValid(Player player) { return !isRemoved() && player.distanceToSqr(
            getBlockPos().getX() + 0.5, getBlockPos().getY() + 0.5, getBlockPos().getZ() + 0.5) <= 64.0; }
    @Override public void clearContent() {
        items.clear();
        for (int index = 0; index < upgrades.slots(); index++) upgrades.setStack(index, ItemStack.EMPTY);
        setChanged();
    }

    private record Operation(String key, FabricationRecipe recipe, int consumePrimary, int consumeSecondary,
            int consumeTertiary, ItemStack output, ItemStack secondaryOutput) { }
    private record Resolution(MachineBlockedReason reason, @Nullable Operation operation) { }
}
