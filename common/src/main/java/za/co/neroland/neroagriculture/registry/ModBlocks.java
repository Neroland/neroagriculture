package za.co.neroland.neroagriculture.registry;

import java.util.List;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

import za.co.neroland.neroagriculture.NeroAgricultureCommon;
import za.co.neroland.neroagriculture.machine.FoundationMachineBlock;
import za.co.neroland.neroagriculture.fluid.ModFluids;
import za.co.neroland.neroagriculture.fluid.NutrientLiquidBlock;
import za.co.neroland.neroagriculture.crop.ProsporaCropBlock;
import za.co.neroland.neroagriculture.crop.ResourceCropBlock;
import za.co.neroland.neroagriculture.crop.SpeciesCropBlock;
import za.co.neroland.neroagriculture.crop.GrowBedBlock;
import za.co.neroland.nerolandcore.registry.RegistrationProvider;
import za.co.neroland.nerolandcore.registry.RegistrationProvider.RegistryEntry;

/** Finite Stage 2 block catalog. Dynamic material identity belongs in data components, not registry ids. */
public final class ModBlocks {
    public static final RegistrationProvider<Block> BLOCKS = RegistrationProvider.get(Registries.BLOCK, NeroAgricultureCommon.MOD_ID);

    public static final RegistryEntry<Block> TERRITE_GROW_BED = BLOCKS.register("territe_grow_bed",
            key -> new Block(BlockBehaviour.Properties.of().setId(key).mapColor(MapColor.PLANT)
                    .strength(2.5F, 6.0F).requiresCorrectToolForDrops().sound(SoundType.METAL).noOcclusion()));
    public static final RegistryEntry<GrowBedBlock> FORGITE_GROW_BED = bed("forgite_grow_bed", za.co.neroland.neroagriculture.content.FragmentTier.FORGITE, MapColor.METAL);
    public static final RegistryEntry<GrowBedBlock> ORBITE_GROW_BED = bed("orbite_grow_bed", za.co.neroland.neroagriculture.content.FragmentTier.ORBITE, MapColor.COLOR_LIGHT_BLUE);
    public static final RegistryEntry<GrowBedBlock> COLONITE_GROW_BED = bed("colonite_grow_bed", za.co.neroland.neroagriculture.content.FragmentTier.COLONITE, MapColor.COLOR_GREEN);
    public static final RegistryEntry<GrowBedBlock> VOIDITE_GROW_BED = bed("voidite_grow_bed", za.co.neroland.neroagriculture.content.FragmentTier.VOIDITE, MapColor.COLOR_PURPLE);
    public static final RegistryEntry<ResourceCropBlock> RESOURCE_CROP = BLOCKS.register("resource_crop",
            key -> new ResourceCropBlock(BlockBehaviour.Properties.of().setId(key).mapColor(MapColor.PLANT)
                    .strength(0.5F).sound(SoundType.CROP).noOcclusion().noCollision().randomTicks().noLootTable()
                    .pushReaction(net.minecraft.world.level.material.PushReaction.BLOCK)));
    // Base crop planted from the Prospora Seed; not in ALL, so no block item (placed via the seed).
    public static final RegistryEntry<ProsporaCropBlock> PROSPORA_CROP = BLOCKS.register("prospora_crop",
            key -> new ProsporaCropBlock(BlockBehaviour.Properties.of().setId(key).mapColor(MapColor.PLANT)
                    .instabreak().sound(SoundType.CROP).noOcclusion().noCollision().randomTicks()
                    .pushReaction(net.minecraft.world.level.material.PushReaction.DESTROY)));
    public static final RegistryEntry<SpeciesCropBlock> ENGINEERED_FOOD_CROP =
            speciesCrop("engineered_food_crop", za.co.neroland.neroagriculture.food.FoodDefinition.Kind.FOOD, MapColor.PLANT);
    public static final RegistryEntry<SpeciesCropBlock> ALIEN_CROP =
            speciesCrop("alien_crop", za.co.neroland.neroagriculture.food.FoodDefinition.Kind.ALIEN, MapColor.COLOR_PURPLE);
    public static final RegistryEntry<Block> GREENHOUSE_FRAME = plain("greenhouse_frame", MapColor.METAL);
    public static final RegistryEntry<za.co.neroland.neroagriculture.greenhouse.GreenhouseGlassBlock> GREENHOUSE_GLASS =
            BLOCKS.register("greenhouse_glass", key -> new za.co.neroland.neroagriculture.greenhouse.GreenhouseGlassBlock(
                    BlockBehaviour.Properties.of().setId(key).mapColor(MapColor.COLOR_LIGHT_BLUE)
                            .strength(2.0F, 6.0F).sound(SoundType.GLASS).noOcclusion()));
    // Not in ALL: needs a DoubleHighBlockItem (registered in ModItems), not the auto plain BlockItem.
    public static final RegistryEntry<za.co.neroland.neroagriculture.greenhouse.GreenhouseDoorBlock> GREENHOUSE_DOOR =
            BLOCKS.register("greenhouse_door", key -> new za.co.neroland.neroagriculture.greenhouse.GreenhouseDoorBlock(
                    BlockBehaviour.Properties.of().setId(key).mapColor(MapColor.METAL).strength(3.0F, 6.0F)
                            .sound(SoundType.METAL).noOcclusion()
                            .pushReaction(net.minecraft.world.level.material.PushReaction.DESTROY)));
    public static final RegistryEntry<Block> CROP_TOWER_FRAME = plain("crop_tower_frame", MapColor.METAL);
    public static final RegistryEntry<Block> TERRITE_FRAGMENT_BLOCK = plain("territe_fragment_block", MapColor.GRASS);
    public static final RegistryEntry<Block> FORGITE_FRAGMENT_BLOCK = plain("forgite_fragment_block", MapColor.METAL);
    public static final RegistryEntry<Block> ORBITE_FRAGMENT_BLOCK = plain("orbite_fragment_block", MapColor.COLOR_LIGHT_BLUE);
    public static final RegistryEntry<Block> COLONITE_FRAGMENT_BLOCK = plain("colonite_fragment_block", MapColor.COLOR_GREEN);
    public static final RegistryEntry<Block> VOIDITE_FRAGMENT_BLOCK = plain("voidite_fragment_block", MapColor.COLOR_PURPLE);
    public static final RegistryEntry<Block> FRAGMENT_DECOR = plain("fragment_decor", MapColor.COLOR_MAGENTA);
    public static final RegistryEntry<NutrientLiquidBlock> NUTRIENT = BLOCKS.register("nutrient",
            key -> new NutrientLiquidBlock((net.minecraft.world.level.material.FlowingFluid) ModFluids.NUTRIENT.get(),
                    BlockBehaviour.Properties.of().setId(key).mapColor(MapColor.COLOR_GREEN).replaceable()
                            .noCollision().strength(100.0F).noLootTable()));
    public static final RegistryEntry<NutrientLiquidBlock> BIOFUEL = BLOCKS.register("biofuel",
            key -> new NutrientLiquidBlock((net.minecraft.world.level.material.FlowingFluid) ModFluids.BIOFUEL.get(),
                    BlockBehaviour.Properties.of().setId(key).mapColor(MapColor.COLOR_BROWN).replaceable()
                            .noCollision().strength(100.0F).noLootTable()));

    public static final RegistryEntry<FoundationMachineBlock> FRAGMENT_EXTRACTOR = machine("fragment_extractor");
    public static final RegistryEntry<FoundationMachineBlock> FRAGMENT_INFUSER = machine("fragment_infuser");
    public static final RegistryEntry<FoundationMachineBlock> SEED_SYNTHESIZER = machine("seed_synthesizer");
    public static final RegistryEntry<za.co.neroland.neroagriculture.automation.AreaMachineBlock> PLANTER = areaMachine("planter");
    public static final RegistryEntry<za.co.neroland.neroagriculture.automation.AreaMachineBlock> HARVESTER = areaMachine("harvester");
    public static final RegistryEntry<FoundationMachineBlock> SEED_RESEARCH_BENCH = machine("seed_research_bench");
    public static final RegistryEntry<za.co.neroland.neroagriculture.greenhouse.GreenhouseControllerBlock> GREENHOUSE_CONTROLLER =
            BLOCKS.register("greenhouse_controller", key -> new za.co.neroland.neroagriculture.greenhouse.GreenhouseControllerBlock(
                    BlockBehaviour.Properties.of().setId(key).mapColor(MapColor.METAL).strength(3.5F, 8.0F)
                            .requiresCorrectToolForDrops().sound(SoundType.METAL).noOcclusion()));
    public static final RegistryEntry<za.co.neroland.neroagriculture.fertiliser.FertiliserProcessorBlock> FERTILISER_PROCESSOR =
            BLOCKS.register("fertiliser_processor", key -> new za.co.neroland.neroagriculture.fertiliser.FertiliserProcessorBlock(
                    BlockBehaviour.Properties.of().setId(key).mapColor(MapColor.METAL).strength(3.5F, 8.0F)
                            .requiresCorrectToolForDrops().sound(SoundType.METAL).noOcclusion()));
    public static final RegistryEntry<za.co.neroland.neroagriculture.automation.AreaMachineBlock> FERTILISER_APPLICATOR = areaMachine("fertiliser_applicator");
    public static final RegistryEntry<za.co.neroland.neroagriculture.genetics.GeneticsStationBlock> GENETICS_STATION =
            BLOCKS.register("genetics_station", key -> new za.co.neroland.neroagriculture.genetics.GeneticsStationBlock(
                    BlockBehaviour.Properties.of().setId(key).mapColor(MapColor.METAL).strength(3.5F, 8.0F)
                            .requiresCorrectToolForDrops().sound(SoundType.METAL).noOcclusion()));
    public static final RegistryEntry<za.co.neroland.neroagriculture.lifesupport.BioreactorBlock> OXYGEN_PLANT =
            BLOCKS.register("oxygen_plant", key -> new za.co.neroland.neroagriculture.lifesupport.BioreactorBlock(
                    BlockBehaviour.Properties.of().setId(key).mapColor(MapColor.METAL).strength(3.5F, 8.0F)
                            .requiresCorrectToolForDrops().sound(SoundType.METAL).noOcclusion()));
    public static final RegistryEntry<za.co.neroland.neroagriculture.biofuel.BiofuelConverterBlock> BIOFUEL_CONVERTER =
            BLOCKS.register("biofuel_converter", key -> new za.co.neroland.neroagriculture.biofuel.BiofuelConverterBlock(
                    BlockBehaviour.Properties.of().setId(key).mapColor(MapColor.METAL).strength(3.5F, 8.0F)
                            .requiresCorrectToolForDrops().sound(SoundType.METAL).noOcclusion()));
    public static final RegistryEntry<za.co.neroland.neroagriculture.tower.CropTowerControllerBlock> CROP_TOWER_CONTROLLER =
            BLOCKS.register("crop_tower_controller", key -> new za.co.neroland.neroagriculture.tower.CropTowerControllerBlock(
                    BlockBehaviour.Properties.of().setId(key).mapColor(MapColor.METAL).strength(3.5F, 8.0F)
                            .requiresCorrectToolForDrops().sound(SoundType.METAL).noOcclusion()));
    public static final RegistryEntry<za.co.neroland.neroagriculture.genetics.PollinationBeaconBlock> POLLINATION_BEACON =
            BLOCKS.register("pollination_beacon", key -> new za.co.neroland.neroagriculture.genetics.PollinationBeaconBlock(
                    BlockBehaviour.Properties.of().setId(key).mapColor(MapColor.METAL).strength(3.5F, 8.0F)
                            .requiresCorrectToolForDrops().sound(SoundType.METAL).noOcclusion()));
    public static final RegistryEntry<za.co.neroland.neroagriculture.terraforming.TerraformingControllerBlock> TERRAFORMING_CONTROLLER =
            BLOCKS.register("terraforming_controller", key -> new za.co.neroland.neroagriculture.terraforming.TerraformingControllerBlock(
                    BlockBehaviour.Properties.of().setId(key).mapColor(MapColor.METAL).strength(3.5F, 8.0F)
                            .requiresCorrectToolForDrops().sound(SoundType.METAL).noOcclusion()));

    public static final List<RegistryEntry<? extends Block>> ALL = List.of(TERRITE_GROW_BED, FORGITE_GROW_BED,
            ORBITE_GROW_BED, COLONITE_GROW_BED, VOIDITE_GROW_BED, RESOURCE_CROP, ENGINEERED_FOOD_CROP,
            ALIEN_CROP, GREENHOUSE_FRAME, GREENHOUSE_GLASS, CROP_TOWER_FRAME, TERRITE_FRAGMENT_BLOCK,
            FORGITE_FRAGMENT_BLOCK, ORBITE_FRAGMENT_BLOCK, COLONITE_FRAGMENT_BLOCK, VOIDITE_FRAGMENT_BLOCK,
            FRAGMENT_DECOR, FRAGMENT_EXTRACTOR, FRAGMENT_INFUSER, SEED_SYNTHESIZER, PLANTER, HARVESTER,
            SEED_RESEARCH_BENCH, GREENHOUSE_CONTROLLER, FERTILISER_PROCESSOR, FERTILISER_APPLICATOR,
            GENETICS_STATION, OXYGEN_PLANT, BIOFUEL_CONVERTER, CROP_TOWER_CONTROLLER, POLLINATION_BEACON,
            TERRAFORMING_CONTROLLER);

    private static RegistryEntry<Block> plain(String name, MapColor color) {
        return BLOCKS.register(name, key -> new Block(BlockBehaviour.Properties.of().setId(key).mapColor(color)
                .strength(2.0F, 6.0F).sound(SoundType.METAL)));
    }

    private static RegistryEntry<za.co.neroland.neroagriculture.automation.AreaMachineBlock> areaMachine(String name) {
        return BLOCKS.register(name, key -> new za.co.neroland.neroagriculture.automation.AreaMachineBlock(
                BlockBehaviour.Properties.of().setId(key).mapColor(MapColor.METAL).strength(3.5F, 8.0F)
                        .requiresCorrectToolForDrops().sound(SoundType.METAL).noOcclusion()));
    }

    private static RegistryEntry<FoundationMachineBlock> machine(String name) {
        return BLOCKS.register(name, key -> new FoundationMachineBlock(BlockBehaviour.Properties.of().setId(key)
                .mapColor(MapColor.METAL).strength(3.5F, 8.0F).requiresCorrectToolForDrops().sound(SoundType.METAL).noOcclusion()));
    }

    private static RegistryEntry<SpeciesCropBlock> speciesCrop(String name,
            za.co.neroland.neroagriculture.food.FoodDefinition.Kind kind, MapColor color) {
        return BLOCKS.register(name, key -> new SpeciesCropBlock(kind, BlockBehaviour.Properties.of().setId(key)
                .mapColor(color).strength(0.5F).sound(SoundType.CROP).noOcclusion().noCollision().randomTicks()
                .noLootTable().pushReaction(net.minecraft.world.level.material.PushReaction.BLOCK)));
    }

    private static RegistryEntry<GrowBedBlock> bed(String name,
            za.co.neroland.neroagriculture.content.FragmentTier tier, MapColor color) {
        return BLOCKS.register(name, key -> new GrowBedBlock(tier, BlockBehaviour.Properties.of().setId(key)
                .mapColor(color).strength(2.5F, 6.0F).requiresCorrectToolForDrops().sound(SoundType.METAL).noOcclusion()));
    }

    public static za.co.neroland.neroagriculture.content.FragmentTier growBedTier(Block block) {
        if (block == TERRITE_GROW_BED.get()) return za.co.neroland.neroagriculture.content.FragmentTier.TERRITE;
        if (block == FORGITE_GROW_BED.get()) return za.co.neroland.neroagriculture.content.FragmentTier.FORGITE;
        if (block == ORBITE_GROW_BED.get()) return za.co.neroland.neroagriculture.content.FragmentTier.ORBITE;
        if (block == COLONITE_GROW_BED.get()) return za.co.neroland.neroagriculture.content.FragmentTier.COLONITE;
        if (block == VOIDITE_GROW_BED.get()) return za.co.neroland.neroagriculture.content.FragmentTier.VOIDITE;
        return null;
    }

    private ModBlocks() { }
    public static void init() { }
}
