package za.co.neroland.neroagriculture.crop;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
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

import za.co.neroland.neroagriculture.config.AgricultureConfig;
import za.co.neroland.neroagriculture.content.FragmentTier;
import za.co.neroland.neroagriculture.fertiliser.FertilisableBed;
import za.co.neroland.neroagriculture.fertiliser.FertiliserDose;
import za.co.neroland.neroagriculture.fertiliser.FertiliserType;
import za.co.neroland.neroagriculture.fluid.ModFluids;
import za.co.neroland.neroagriculture.registry.ModBlockEntities;
import za.co.neroland.nerolandcore.fluid.FluidBuffer;
import za.co.neroland.nerolandcore.fluid.NeroFluidStorage;
import za.co.neroland.nerolandcore.machine.AbstractMachineBlockEntity;
import za.co.neroland.nerolandcore.sideconfig.Channel;
import za.co.neroland.nerolandcore.sideconfig.SideConfig;
import za.co.neroland.nerolandcore.sideconfig.SideMode;

/** Persistent NF/nutrient storage exposed through Core's cross-loader capabilities. */
public final class GrowBedBlockEntity extends AbstractMachineBlockEntity
        implements FertilisableBed, WorldlyContainer, MenuProvider {
    public static final int SEED_SLOT = 0;
    public static final int OUTPUT_START = 1;
    public static final int SLOT_COUNT = 5;
    private static final int[] SLOTS = {0, 1, 2, 3, 4};
    private final FluidBuffer nutrient;
    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private int plantTimer;
    @org.jetbrains.annotations.Nullable private FertiliserDose speedDose;
    @org.jetbrains.annotations.Nullable private FertiliserDose yieldDose;

    public GrowBedBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.GROW_BED.get(), pos, state, AgricultureConfig.MACHINE_ENERGY_CAPACITY.get(),
                AgricultureConfig.MACHINE_ENERGY_RATE.get(), 0, stack -> null);
        this.nutrient = new FluidBuffer(AgricultureConfig.MACHINE_FLUID_CAPACITY.get(), this::changedAndSync);
        installSideConfig(SideConfig.builder()
                .channel(Channel.ITEM, za.co.neroland.nerolandcore.sideconfig.SlotGroup.of("input", SEED_SLOT),
                        za.co.neroland.nerolandcore.sideconfig.SlotGroup.of("output", 1, 2, 3, 4))
                .channel(Channel.ENERGY).channel(Channel.FLUID)
                .allow(Channel.ENERGY, SideMode.OUTPUT, false).allow(Channel.ENERGY, SideMode.IO, false)
                .allow(Channel.FLUID, SideMode.OUTPUT, false).allow(Channel.FLUID, SideMode.IO, false).build())
                .withItems(() -> this).withFluid(this::getFluid);
    }

    private final ContainerData menuData = new ContainerData() {
        @Override public int get(int index) {
            return switch (index) {
                case 0 -> (int) Math.min(Integer.MAX_VALUE, getEnergy().getAmount());
                case 1 -> (int) Math.min(Integer.MAX_VALUE, nutrient.getAmount());
                case 2 -> tier().ordinal();
                default -> 0;
            };
        }
        @Override public void set(int index, int value) { }
        @Override public int getCount() { return za.co.neroland.neroagriculture.menu.GrowBedMenu.DATA_COUNT; }
    };

    @Override public Component getDisplayName() { return getBlockState().getBlock().getName(); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new za.co.neroland.neroagriculture.menu.GrowBedMenu(id, inventory, this, menuData, worldPosition);
    }

    /**
     * Once a second: auto-plant the slotted seed above this bed, and auto-harvest a mature crop into the
     * bed's own output slots (Botany-Pots style) so hoppers can automate the whole loop.
     */
    public static void tick(Level level, BlockPos pos, BlockState state, GrowBedBlockEntity bed) {
        AbstractMachineBlockEntity.tick(level, pos, state, bed);
        if (!(level instanceof ServerLevel serverLevel) || ++bed.plantTimer < 20) return;
        bed.plantTimer = 0;
        bed.autoHarvest(serverLevel, pos);
        bed.autoPlant(serverLevel, pos);
    }

    private void autoPlant(ServerLevel level, BlockPos pos) {
        ItemStack seed = items.get(SEED_SLOT);
        if (seed.isEmpty() || !seed.is(za.co.neroland.neroagriculture.registry.ModItems.RESOURCE_SEED.get())) return;
        if (!level.getBlockState(pos.above()).canBeReplaced()) return;
        Player nearest = level.getNearestPlayer(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 32.0, false);
        if (!(nearest instanceof ServerPlayer player)) return;
        if (za.co.neroland.neroagriculture.content.ResourceSeedItem.tryPlantAbove(level, player, pos, seed)) {
            setChanged();
        }
    }

    private void autoHarvest(ServerLevel level, BlockPos pos) {
        java.util.List<ItemStack> drops = ResourceCropBlock.harvestToStorage(level, pos.above());
        if (drops.isEmpty()) return;
        for (ItemStack drop : drops) {
            ItemStack remaining = insertOutput(drop);
            if (!remaining.isEmpty()) {
                net.minecraft.world.level.block.Block.popResource(level, pos.above(), remaining);
            }
        }
        setChanged();
    }

    private ItemStack insertOutput(ItemStack stack) {
        for (int slot = OUTPUT_START; slot < SLOT_COUNT && !stack.isEmpty(); slot++) {
            ItemStack existing = items.get(slot);
            if (existing.isEmpty()) {
                items.set(slot, stack);
                return ItemStack.EMPTY;
            }
            if (ItemStack.isSameItemSameComponents(existing, stack)) {
                int move = Math.min(stack.getCount(), existing.getMaxStackSize() - existing.getCount());
                existing.grow(move);
                stack.shrink(move);
            }
        }
        return stack;
    }

    // --- seed + output slots (WorldlyContainer) -----------------------------------------------
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
        return slot == SEED_SLOT && stack.is(za.co.neroland.neroagriculture.registry.ModItems.RESOURCE_SEED.get());
    }
    @Override public int[] getSlotsForFace(Direction side) { return SLOTS.clone(); }
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @org.jetbrains.annotations.Nullable Direction side) {
        return canPlaceItem(slot, stack);
    }
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) { return slot >= OUTPUT_START; }
    @Override public void clearContent() { items.clear(); setChanged(); }

    public FragmentTier tier() {
        return getBlockState().getBlock() instanceof GrowBedBlock bed ? bed.tier() : FragmentTier.FORGITE;
    }
    public NeroFluidStorage getFluid() { return nutrient; }
    public boolean hasGrowthResources() {
        int energyCost = AgricultureConfig.GROW_BED_ENERGY_COST.get();
        int fluidCost = AgricultureConfig.GROW_BED_NUTRIENT_COST.get();
        return GrowBedResources.has(getEnergy(), nutrient, ModFluids.NUTRIENT.get(), energyCost, fluidCost);
    }
    public boolean consumeGrowthResources() {
        if (!GrowBedResources.consume(getEnergy(), nutrient, ModFluids.NUTRIENT.get(),
                AgricultureConfig.GROW_BED_ENERGY_COST.get(), AgricultureConfig.GROW_BED_NUTRIENT_COST.get())) return false;
        changedAndSync();
        return true;
    }

    @Override public boolean applyFertiliser(FertiliserType type, int amount, long now, int durationTicks, int maxDose) {
        FertiliserDose current = type == FertiliserType.SPEED ? speedDose : yieldDose;
        FertiliserDose next = FertiliserDose.applied(current, type, amount, now, durationTicks, maxDose);
        if (type == FertiliserType.SPEED) speedDose = next; else yieldDose = next;
        changedAndSync();
        return true;
    }

    @Override public FertiliserDose activeDose(FertiliserType type, long now) {
        FertiliserDose dose = type == FertiliserType.SPEED ? speedDose : yieldDose;
        return dose != null && dose.active(now) ? dose : null;
    }

    private void changedAndSync() {
        setChanged();
    }

    @Override public void setChanged() {
        super.setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    @Override protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, items);
        output.putString("NutrientFluid", BuiltInRegistries.FLUID.getKey(nutrient.getRawFluid()).toString());
        output.putInt("NutrientAmount", nutrient.getRawAmount());
        saveDose(output, "Speed", speedDose);
        saveDose(output, "Yield", yieldDose);
    }

    private static void saveDose(ValueOutput output, String key, @org.jetbrains.annotations.Nullable FertiliserDose dose) {
        if (dose == null) return;
        output.putInt(key + "Dose", dose.amount());
        output.putLong(key + "Expiry", dose.expiryTick());
    }
    @Override protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        ContainerHelper.loadAllItems(input, items);
        Fluid fluid = BuiltInRegistries.FLUID.getValue(Identifier.parse(
                input.getStringOr("NutrientFluid", "minecraft:empty")));
        nutrient.setRaw(fluid == null ? Fluids.EMPTY : fluid, input.getIntOr("NutrientAmount", 0));
        speedDose = loadDose(input, "Speed", FertiliserType.SPEED);
        yieldDose = loadDose(input, "Yield", FertiliserType.YIELD);
    }

    @org.jetbrains.annotations.Nullable
    private static FertiliserDose loadDose(ValueInput input, String key, FertiliserType type) {
        int amount = input.getIntOr(key + "Dose", 0);
        if (amount <= 0) return null;
        return new FertiliserDose(type, amount, input.getLongOr(key + "Expiry", 0L));
    }
    @Override public Packet<ClientGamePacketListener> getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveCustomOnly(registries); }
}
