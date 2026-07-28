package za.co.neroland.neroagriculture.terraforming;

import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
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

import za.co.neroland.neroagriculture.api.AgricultureApi;
import za.co.neroland.neroagriculture.automation.AutomationOwner;
import za.co.neroland.neroagriculture.automation.AutomationPolicy;
import za.co.neroland.neroagriculture.automation.ErasedOwners;
import za.co.neroland.neroagriculture.config.AgricultureConfig;
import za.co.neroland.neroagriculture.fluid.ModFluids;
import za.co.neroland.neroagriculture.machine.SideConfigMigration;
import za.co.neroland.neroagriculture.menu.StatusMenu;
import za.co.neroland.neroagriculture.registry.ModBlockEntities;
import za.co.neroland.neroagriculture.registry.ModItems;
import za.co.neroland.nerolandcore.fluid.FluidBuffer;
import za.co.neroland.nerolandcore.fluid.NeroFluidStorage;
import za.co.neroland.nerolandcore.machine.AbstractMachineBlockEntity;
import za.co.neroland.nerolandcore.sideconfig.Channel;
import za.co.neroland.nerolandcore.sideconfig.SideConfig;
import za.co.neroland.nerolandcore.sideconfig.SideMode;
import za.co.neroland.nerolandcore.sideconfig.SidePreset;

/**
 * Terraforming controller. A planted Terraforming Seed starts a rate-limited project that spends NF and
 * Nutrient to progress through staged transitions; on completion it registers a bounded region override that
 * makes the area habitable in Agriculture's environment model. It never mutates world blocks or forces
 * chunks, respects the claim/owner policy (fail-closed), and rolls back cleanly. Progress/completion/rollback
 * are published through the public {@link AgricultureApi} terraforming events.
 */
public final class TerraformingControllerBlockEntity extends AbstractMachineBlockEntity
        implements AutomationOwner.Owned, MenuProvider {
    private final FluidBuffer nutrient;
    private boolean seeded;
    private int progress;
    @Nullable private UUID owner;
    private TerraformingStage stage = TerraformingStage.DORMANT;
    private boolean restored;

    public TerraformingControllerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TERRAFORMING_CONTROLLER.get(), pos, state, AgricultureConfig.MACHINE_ENERGY_CAPACITY.get(),
                AgricultureConfig.MACHINE_ENERGY_RATE.get(), 0, stack -> null);
        this.nutrient = new FluidBuffer(AgricultureConfig.MACHINE_FLUID_CAPACITY.get(), this::setChanged);
        installSideConfig(SideConfig.builder().channel(Channel.ENERGY).channel(Channel.FLUID)
                .defaultPreset(SidePreset.PROCESSOR)
                .allow(Channel.ENERGY, SideMode.OUTPUT, false).allow(Channel.FLUID, SideMode.OUTPUT, false).build())
                .withFluid(this::getFluid);
    }

    public NeroFluidStorage getFluid() { return nutrient; }
    public TerraformingStage stage() { return stage; }
    public int progress() { return progress; }

    private final ContainerData menuData = new ContainerData() {
        @Override public int get(int index) {
            int total = AgricultureConfig.TERRAFORM_TOTAL_PROGRESS.get();
            return switch (index) {
                case StatusMenu.MACHINE_ID -> StatusMenu.ID_TERRAFORMING;
                // Permille fraction: ContainerData syncs shorts and the capacity can exceed 32,767.
                case StatusMenu.ENERGY -> za.co.neroland.neroagriculture.menu.GaugeData.permille(
                        getEnergy().getAmount(), getEnergy().getCapacity());
                case StatusMenu.V0 -> total <= 0 ? 100 : (int) (100L * progress / total);
                case StatusMenu.V1 -> stage.ordinal();
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
    @Override public @Nullable UUID automationOwner() { return owner; }
    @Override public void clearAutomationOwner() { owner = null; setChanged(); }

    public static void tick(Level level, BlockPos pos, BlockState state, TerraformingControllerBlockEntity be) {
        AbstractMachineBlockEntity.tick(level, pos, state, be);
        SideConfigMigration.tick(be);
        if (!(level instanceof ServerLevel serverLevel)) return;
        if (!be.restored) {
            be.restored = true;
            be.restoreRegion(serverLevel);
        }
        be.advance(serverLevel);
    }

    private void advance(ServerLevel level) {
        int total = AgricultureConfig.TERRAFORM_TOTAL_PROGRESS.get();
        TerraformingStage previous = stage;
        stage = TerraformingStage.of(seeded, progress, total);
        if (!seeded || stage.complete() || !authorized(level)) {
            if (stage != previous) setChanged();
            return;
        }
        int energy = AgricultureConfig.TERRAFORM_ENERGY_PER_TICK.get();
        int nutrientCost = AgricultureConfig.TERRAFORM_NUTRIENT_PER_TICK.get();
        if (energy > 0 && getEnergy().extract(energy, true) < energy) return;
        if (nutrientCost > 0 && (nutrient.getFluid() != ModFluids.NUTRIENT.get() || nutrient.drain(nutrientCost, true) < nutrientCost)) return;
        if (energy > 0) getEnergy().extract(energy, false);
        if (nutrientCost > 0) nutrient.drain(nutrientCost, false);
        int perTick = AgricultureConfig.TERRAFORM_PROGRESS_PER_TICK.get();
        if (droneAssisted(level)) perTick *= 2; // bounded optional assistance
        progress = Math.min(total, progress + Math.max(1, perTick));
        stage = TerraformingStage.of(seeded, progress, total);
        if (stage != previous) {
            fireEvent(level, (float) progress / total);
            if (stage.complete()) {
                TerraformingRegions.complete(level, worldPosition, AgricultureConfig.TERRAFORM_RADIUS.get());
            }
        }
        setChanged();
    }

    /** Begin a project from a Terraforming Seed in hand; returns true if it started. */
    public boolean start(ServerLevel level, ServerPlayer player, ItemStack held) {
        if (seeded || !held.is(ModItems.TERRAFORMING_SEED.get())) return false;
        if (!AgricultureConfig.TERRAFORM_ENABLED.get()) return false;
        // Authorise against the candidate owner but only RECORD the UUID once authorisation succeeds,
        // so a denied attempt never persists the player's UUID on the block entity.
        UUID candidate = AutomationOwner.trackingEnabled() ? player.getUUID() : null;
        if (!AutomationPolicy.mayEdit(level, worldPosition, candidate)) return false;
        this.owner = candidate;
        seeded = true;
        progress = 0;
        stage = TerraformingStage.SEEDED;
        if (!player.getAbilities().instabuild) held.shrink(1);
        fireEvent(level, 0.0F);
        setChanged();
        return true;
    }

    /** Cancel/roll back the project, removing any region override and restoring the baseline environment. */
    public boolean rollback(ServerLevel level, ServerPlayer player) {
        if (!seeded) return false;
        // Owner-only via interaction; breaking the controller also rolls back, so admins can always undo.
        if (owner != null && !owner.equals(player.getUUID())) return false;
        seeded = false;
        progress = 0;
        stage = TerraformingStage.DORMANT;
        TerraformingRegions.rollback(level, worldPosition);
        fireEvent(level, 0.0F);
        setChanged();
        return true;
    }

    private boolean authorized(ServerLevel level) {
        return AgricultureConfig.TERRAFORM_ENABLED.get() && AutomationPolicy.mayEdit(level, worldPosition, owner);
    }

    private boolean droneAssisted(ServerLevel level) {
        if (AgricultureApi.DRONES.isEmpty()) return false;
        ServerPlayer player = owner == null ? null : level.getServer().getPlayerList().getPlayer(owner);
        if (player == null) return false;
        AgricultureApi.DroneRequest request = new AgricultureApi.DroneRequest(player,
                Identifier.fromNamespaceAndPath("neroagriculture", "terraforming"), worldPosition.asLong(),
                AgricultureConfig.TERRAFORM_RADIUS.get());
        for (AgricultureApi.DroneAssistanceProvider provider : AgricultureApi.DRONES) {
            if (provider.assist(request)) return true;
        }
        return false;
    }

    private void fireEvent(ServerLevel level, float fraction) {
        AgricultureApi.fireTerraforming(new AgricultureApi.TerraformingEvent(
                Identifier.fromNamespaceAndPath("neroagriculture", "project/" + Long.toHexString(worldPosition.asLong())),
                level.dimension().identifier(), worldPosition.asLong(), Math.max(0.0F, Math.min(1.0F, fraction))));
    }

    public String status() {
        int total = AgricultureConfig.TERRAFORM_TOTAL_PROGRESS.get();
        int percent = total <= 0 ? 100 : (int) (100L * progress / total);
        return stage.name().toLowerCase() + " " + percent + "% radius=" + AgricultureConfig.TERRAFORM_RADIUS.get()
                + " owner=" + (owner == null ? "none" : "set") + (AgricultureConfig.TERRAFORM_ENABLED.get() ? "" : " (disabled)");
    }

    @Override public void setRemoved() {
        if (level instanceof ServerLevel serverLevel) TerraformingRegions.rollback(serverLevel, worldPosition);
        AutomationOwner.untrack(this);
        super.setRemoved();
    }
    @Override public void clearRemoved() {
        super.clearRemoved();
        AutomationOwner.track(this);
        if (owner != null && level instanceof ServerLevel serverLevel) {
            owner = ErasedOwners.filter(owner, serverLevel.getServer());
        }
    }

    @Override protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putString("NutrientFluid", BuiltInRegistries.FLUID.getKey(nutrient.getRawFluid()).toString());
        output.putInt("NutrientAmount", nutrient.getRawAmount());
        output.putInt("Seeded", seeded ? 1 : 0);
        output.putInt("Progress", progress);
        AutomationOwner.save(output, owner);
        SideConfigMigration.save(output);
    }

    @Override protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        SideConfigMigration.load(this, input);
        Fluid fluid = BuiltInRegistries.FLUID.getValue(Identifier.parse(input.getStringOr("NutrientFluid", "minecraft:empty")));
        nutrient.setRaw(fluid == null ? Fluids.EMPTY : fluid, input.getIntOr("NutrientAmount", 0));
        seeded = input.getBooleanOr("Seeded", false);
        progress = Math.max(0, input.getIntOr("Progress", 0));
        owner = AutomationOwner.load(input);
        stage = TerraformingStage.of(seeded, progress, AgricultureConfig.TERRAFORM_TOTAL_PROGRESS.get());
    }

    /** Re-register a completed region after load (called once the block entity has a level). */
    public void restoreRegion(ServerLevel level) {
        if (stage.complete()) TerraformingRegions.complete(level, worldPosition, AgricultureConfig.TERRAFORM_RADIUS.get());
    }
}
