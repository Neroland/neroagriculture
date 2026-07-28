package za.co.neroland.neroagriculture.registry;

import java.util.List;
import java.util.stream.Collectors;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;

import za.co.neroland.neroagriculture.NeroAgricultureCommon;
import za.co.neroland.neroagriculture.machine.FoundationMachineBlockEntity;
import za.co.neroland.neroagriculture.crop.ResourceCropBlockEntity;
import za.co.neroland.neroagriculture.crop.SpeciesCropBlockEntity;
import za.co.neroland.neroagriculture.crop.GrowBedBlockEntity;
import za.co.neroland.nerolandcore.machine.AbstractMachineBlockEntity;
import za.co.neroland.nerolandcore.registry.RegistrationProvider;
import za.co.neroland.nerolandcore.registry.RegistrationProvider.RegistryEntry;

/** One compatible shell type spans the stable machine blocks until later stages specialize behavior. */
public final class ModBlockEntities {
    public static final RegistrationProvider<BlockEntityType<?>> BLOCK_ENTITIES =
            RegistrationProvider.get(Registries.BLOCK_ENTITY_TYPE, NeroAgricultureCommon.MOD_ID);

    public static final RegistryEntry<BlockEntityType<FoundationMachineBlockEntity>> FOUNDATION_MACHINE =
            BLOCK_ENTITIES.register("foundation_machine", key -> new BlockEntityType<>(FoundationMachineBlockEntity::new,
                    ModBlocks.ALL.stream().filter(entry -> entry.get() instanceof za.co.neroland.neroagriculture.machine.FoundationMachineBlock)
                            .map(RegistryEntry::get).collect(Collectors.toSet())));

    public static final RegistryEntry<BlockEntityType<ResourceCropBlockEntity>> RESOURCE_CROP =
            BLOCK_ENTITIES.register("resource_crop", key -> new BlockEntityType<>(ResourceCropBlockEntity::new,
                    java.util.Set.of(ModBlocks.RESOURCE_CROP.get())));

    public static final RegistryEntry<BlockEntityType<SpeciesCropBlockEntity>> SPECIES_CROP =
            BLOCK_ENTITIES.register("species_crop", key -> new BlockEntityType<>(SpeciesCropBlockEntity::new,
                    java.util.Set.of(ModBlocks.ENGINEERED_FOOD_CROP.get(), ModBlocks.ALIEN_CROP.get())));

    public static final RegistryEntry<BlockEntityType<GrowBedBlockEntity>> GROW_BED =
            BLOCK_ENTITIES.register("grow_bed", key -> new BlockEntityType<>(GrowBedBlockEntity::new,
                    java.util.Set.of(ModBlocks.FORGITE_GROW_BED.get(), ModBlocks.ORBITE_GROW_BED.get(),
                            ModBlocks.COLONITE_GROW_BED.get(), ModBlocks.VOIDITE_GROW_BED.get())));

    public static final RegistryEntry<BlockEntityType<za.co.neroland.neroagriculture.automation.AreaMachineBlockEntity>> AREA_MACHINE =
            BLOCK_ENTITIES.register("area_machine", key -> new BlockEntityType<>(
                    za.co.neroland.neroagriculture.automation.AreaMachineBlockEntity::new,
                    java.util.Set.of(ModBlocks.PLANTER.get(), ModBlocks.HARVESTER.get(), ModBlocks.FERTILISER_APPLICATOR.get())));

    public static final RegistryEntry<BlockEntityType<za.co.neroland.neroagriculture.fertiliser.FertiliserProcessorBlockEntity>> FERTILISER_PROCESSOR =
            BLOCK_ENTITIES.register("fertiliser_processor", key -> new BlockEntityType<>(
                    za.co.neroland.neroagriculture.fertiliser.FertiliserProcessorBlockEntity::new,
                    java.util.Set.of(ModBlocks.FERTILISER_PROCESSOR.get())));

    public static final RegistryEntry<BlockEntityType<za.co.neroland.neroagriculture.greenhouse.GreenhouseControllerBlockEntity>> GREENHOUSE_CONTROLLER =
            BLOCK_ENTITIES.register("greenhouse_controller", key -> new BlockEntityType<>(
                    za.co.neroland.neroagriculture.greenhouse.GreenhouseControllerBlockEntity::new,
                    java.util.Set.of(ModBlocks.GREENHOUSE_CONTROLLER.get())));

    public static final RegistryEntry<BlockEntityType<za.co.neroland.neroagriculture.genetics.GeneticsStationBlockEntity>> GENETICS_STATION =
            BLOCK_ENTITIES.register("genetics_station", key -> new BlockEntityType<>(
                    za.co.neroland.neroagriculture.genetics.GeneticsStationBlockEntity::new,
                    java.util.Set.of(ModBlocks.GENETICS_STATION.get())));

    public static final RegistryEntry<BlockEntityType<za.co.neroland.neroagriculture.genetics.PollinationBeaconBlockEntity>> POLLINATION_BEACON =
            BLOCK_ENTITIES.register("pollination_beacon", key -> new BlockEntityType<>(
                    za.co.neroland.neroagriculture.genetics.PollinationBeaconBlockEntity::new,
                    java.util.Set.of(ModBlocks.POLLINATION_BEACON.get())));

    public static final RegistryEntry<BlockEntityType<za.co.neroland.neroagriculture.terraforming.TerraformingControllerBlockEntity>> TERRAFORMING_CONTROLLER =
            BLOCK_ENTITIES.register("terraforming_controller", key -> new BlockEntityType<>(
                    za.co.neroland.neroagriculture.terraforming.TerraformingControllerBlockEntity::new,
                    java.util.Set.of(ModBlocks.TERRAFORMING_CONTROLLER.get())));

    public static final RegistryEntry<BlockEntityType<za.co.neroland.neroagriculture.tower.CropTowerControllerBlockEntity>> CROP_TOWER_CONTROLLER =
            BLOCK_ENTITIES.register("crop_tower_controller", key -> new BlockEntityType<>(
                    za.co.neroland.neroagriculture.tower.CropTowerControllerBlockEntity::new,
                    java.util.Set.of(ModBlocks.CROP_TOWER_CONTROLLER.get())));

    public static final RegistryEntry<BlockEntityType<za.co.neroland.neroagriculture.biofuel.BiofuelConverterBlockEntity>> BIOFUEL_CONVERTER =
            BLOCK_ENTITIES.register("biofuel_converter", key -> new BlockEntityType<>(
                    za.co.neroland.neroagriculture.biofuel.BiofuelConverterBlockEntity::new,
                    java.util.Set.of(ModBlocks.BIOFUEL_CONVERTER.get())));

    public static final RegistryEntry<BlockEntityType<za.co.neroland.neroagriculture.lifesupport.BioreactorBlockEntity>> BIOREACTOR =
            BLOCK_ENTITIES.register("bioreactor", key -> new BlockEntityType<>(
                    za.co.neroland.neroagriculture.lifesupport.BioreactorBlockEntity::new,
                    java.util.Set.of(ModBlocks.OXYGEN_PLANT.get())));

    /**
     * The canonical list of machine block-entity types (every type whose BE extends Core's
     * {@link AbstractMachineBlockEntity}). The per-loader capability registrations iterate this, so a
     * new machine added here gets its energy/fluid/item surfaces on every loader without touching the
     * loader modules. Only callable once block-entity registration has run.
     */
    public static List<BlockEntityType<? extends AbstractMachineBlockEntity>> machineTypes() {
        return List.of(
                FOUNDATION_MACHINE.get(),
                GROW_BED.get(),
                GENETICS_STATION.get(),
                AREA_MACHINE.get(),
                CROP_TOWER_CONTROLLER.get(),
                GREENHOUSE_CONTROLLER.get(),
                BIOREACTOR.get(),
                BIOFUEL_CONVERTER.get(),
                FERTILISER_PROCESSOR.get(),
                POLLINATION_BEACON.get(),
                TERRAFORMING_CONTROLLER.get());
    }

    private ModBlockEntities() { }
    public static void init() { }
}
