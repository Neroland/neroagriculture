package za.co.neroland.neroagriculture.registry;

import java.util.stream.Collectors;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;

import za.co.neroland.neroagriculture.NeroAgricultureCommon;
import za.co.neroland.neroagriculture.machine.FoundationMachineBlockEntity;
import za.co.neroland.neroagriculture.crop.ResourceCropBlockEntity;
import za.co.neroland.neroagriculture.crop.SpeciesCropBlockEntity;
import za.co.neroland.neroagriculture.crop.GrowBedBlockEntity;
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
                    java.util.Set.of(ModBlocks.INDUSTRIAL_GROW_BED.get(), ModBlocks.ORBITAL_GROW_BED.get(),
                            ModBlocks.COLONIAL_GROW_BED.get(), ModBlocks.DEEPVOID_GROW_BED.get())));

    public static final RegistryEntry<BlockEntityType<za.co.neroland.neroagriculture.greenhouse.GreenhouseControllerBlockEntity>> GREENHOUSE_CONTROLLER =
            BLOCK_ENTITIES.register("greenhouse_controller", key -> new BlockEntityType<>(
                    za.co.neroland.neroagriculture.greenhouse.GreenhouseControllerBlockEntity::new,
                    java.util.Set.of(ModBlocks.GREENHOUSE_CONTROLLER.get())));

    private ModBlockEntities() { }
    public static void init() { }
}
