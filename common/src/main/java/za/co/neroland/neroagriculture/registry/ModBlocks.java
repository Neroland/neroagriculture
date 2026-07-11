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
import za.co.neroland.nerolandcore.registry.RegistrationProvider;
import za.co.neroland.nerolandcore.registry.RegistrationProvider.RegistryEntry;

/** Finite Stage 2 block catalog. Dynamic material identity belongs in data components, not registry ids. */
public final class ModBlocks {
    public static final RegistrationProvider<Block> BLOCKS = RegistrationProvider.get(Registries.BLOCK, NeroAgricultureCommon.MOD_ID);

    public static final RegistryEntry<Block> TERRAN_GROW_BED = plain("terran_grow_bed", MapColor.PLANT);
    public static final RegistryEntry<Block> INDUSTRIAL_GROW_BED = plain("industrial_grow_bed", MapColor.METAL);
    public static final RegistryEntry<Block> ORBITAL_GROW_BED = plain("orbital_grow_bed", MapColor.COLOR_LIGHT_BLUE);
    public static final RegistryEntry<Block> COLONIAL_GROW_BED = plain("colonial_grow_bed", MapColor.COLOR_GREEN);
    public static final RegistryEntry<Block> DEEPVOID_GROW_BED = plain("deepvoid_grow_bed", MapColor.COLOR_PURPLE);
    public static final RegistryEntry<Block> RESOURCE_CROP = plain("resource_crop", MapColor.PLANT);
    public static final RegistryEntry<Block> ENGINEERED_FOOD_CROP = plain("engineered_food_crop", MapColor.PLANT);
    public static final RegistryEntry<Block> ALIEN_CROP = plain("alien_crop", MapColor.COLOR_PURPLE);
    public static final RegistryEntry<Block> GREENHOUSE_FRAME = plain("greenhouse_frame", MapColor.METAL);
    public static final RegistryEntry<Block> GREENHOUSE_GLASS = plain("greenhouse_glass", MapColor.COLOR_LIGHT_BLUE);
    public static final RegistryEntry<Block> CROP_TOWER_FRAME = plain("crop_tower_frame", MapColor.METAL);
    public static final RegistryEntry<Block> TERRAN_ESSENCE_BLOCK = plain("terran_essence_block", MapColor.GRASS);
    public static final RegistryEntry<Block> INDUSTRIAL_ESSENCE_BLOCK = plain("industrial_essence_block", MapColor.METAL);
    public static final RegistryEntry<Block> ORBITAL_ESSENCE_BLOCK = plain("orbital_essence_block", MapColor.COLOR_LIGHT_BLUE);
    public static final RegistryEntry<Block> COLONIAL_ESSENCE_BLOCK = plain("colonial_essence_block", MapColor.COLOR_GREEN);
    public static final RegistryEntry<Block> DEEPVOID_ESSENCE_BLOCK = plain("deepvoid_essence_block", MapColor.COLOR_PURPLE);
    public static final RegistryEntry<Block> ESSENCE_DECOR = plain("essence_decor", MapColor.COLOR_MAGENTA);
    public static final RegistryEntry<NutrientLiquidBlock> NUTRIENT = BLOCKS.register("nutrient",
            key -> new NutrientLiquidBlock((net.minecraft.world.level.material.FlowingFluid) ModFluids.NUTRIENT.get(),
                    BlockBehaviour.Properties.of().setId(key).mapColor(MapColor.COLOR_GREEN).replaceable()
                            .noCollision().strength(100.0F).noLootTable()));

    public static final RegistryEntry<FoundationMachineBlock> ESSENCE_EXTRACTOR = machine("essence_extractor");
    public static final RegistryEntry<FoundationMachineBlock> ESSENCE_INFUSER = machine("essence_infuser");
    public static final RegistryEntry<FoundationMachineBlock> SEED_SYNTHESIZER = machine("seed_synthesizer");
    public static final RegistryEntry<FoundationMachineBlock> PLANTER = machine("planter");
    public static final RegistryEntry<FoundationMachineBlock> HARVESTER = machine("harvester");
    public static final RegistryEntry<FoundationMachineBlock> SEED_RESEARCH_BENCH = machine("seed_research_bench");
    public static final RegistryEntry<FoundationMachineBlock> GREENHOUSE_CONTROLLER = machine("greenhouse_controller");
    public static final RegistryEntry<FoundationMachineBlock> FERTILISER_PROCESSOR = machine("fertiliser_processor");
    public static final RegistryEntry<FoundationMachineBlock> FERTILISER_APPLICATOR = machine("fertiliser_applicator");
    public static final RegistryEntry<FoundationMachineBlock> GENETICS_STATION = machine("genetics_station");
    public static final RegistryEntry<FoundationMachineBlock> OXYGEN_PLANT = machine("oxygen_plant");
    public static final RegistryEntry<FoundationMachineBlock> BIOFUEL_CONVERTER = machine("biofuel_converter");
    public static final RegistryEntry<FoundationMachineBlock> CROP_TOWER_CONTROLLER = machine("crop_tower_controller");
    public static final RegistryEntry<FoundationMachineBlock> POLLINATION_BEACON = machine("pollination_beacon");
    public static final RegistryEntry<FoundationMachineBlock> TERRAFORMING_CONTROLLER = machine("terraforming_controller");

    public static final List<RegistryEntry<? extends Block>> ALL = List.of(TERRAN_GROW_BED, INDUSTRIAL_GROW_BED,
            ORBITAL_GROW_BED, COLONIAL_GROW_BED, DEEPVOID_GROW_BED, RESOURCE_CROP, ENGINEERED_FOOD_CROP,
            ALIEN_CROP, GREENHOUSE_FRAME, GREENHOUSE_GLASS, CROP_TOWER_FRAME, TERRAN_ESSENCE_BLOCK,
            INDUSTRIAL_ESSENCE_BLOCK, ORBITAL_ESSENCE_BLOCK, COLONIAL_ESSENCE_BLOCK, DEEPVOID_ESSENCE_BLOCK,
            ESSENCE_DECOR, ESSENCE_EXTRACTOR, ESSENCE_INFUSER, SEED_SYNTHESIZER, PLANTER, HARVESTER,
            SEED_RESEARCH_BENCH, GREENHOUSE_CONTROLLER, FERTILISER_PROCESSOR, FERTILISER_APPLICATOR,
            GENETICS_STATION, OXYGEN_PLANT, BIOFUEL_CONVERTER, CROP_TOWER_CONTROLLER, POLLINATION_BEACON,
            TERRAFORMING_CONTROLLER);

    private static RegistryEntry<Block> plain(String name, MapColor color) {
        return BLOCKS.register(name, key -> new Block(BlockBehaviour.Properties.of().setId(key).mapColor(color)
                .strength(2.0F, 6.0F).sound(SoundType.METAL)));
    }

    private static RegistryEntry<FoundationMachineBlock> machine(String name) {
        return BLOCKS.register(name, key -> new FoundationMachineBlock(BlockBehaviour.Properties.of().setId(key)
                .mapColor(MapColor.METAL).strength(3.5F, 8.0F).requiresCorrectToolForDrops().sound(SoundType.METAL)));
    }

    private ModBlocks() { }
    public static void init() { }
}
