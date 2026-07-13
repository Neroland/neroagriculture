package za.co.neroland.neroagriculture.command;

import java.util.ArrayList;
import java.util.List;

import com.mojang.brigadier.Command;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import za.co.neroland.neroagriculture.NeroAgricultureCommon;
import za.co.neroland.neroagriculture.content.FragmentTier;
import za.co.neroland.neroagriculture.content.MaterialVariant;
import za.co.neroland.neroagriculture.crop.CropVariantState;
import za.co.neroland.neroagriculture.crop.GrowBedBlockEntity;
import za.co.neroland.neroagriculture.crop.ResourceCropBlock;
import za.co.neroland.neroagriculture.crop.ResourceCropBlockEntity;
import za.co.neroland.neroagriculture.fluid.ModFluids;
import za.co.neroland.nerolandcore.fluid.FluidBuffer;
import za.co.neroland.neroagriculture.registry.ModBlocks;
import za.co.neroland.neroagriculture.registry.ModDataComponents;
import za.co.neroland.neroagriculture.registry.ModItems;
import za.co.neroland.nerolandcore.energy.EnergyBuffer;
import za.co.neroland.nerolandcore.machine.AbstractMachineBlockEntity;

/**
 * Creative-only showcase command. {@code /neroagriculture gallery} builds a display around the invoking
 * player: a floating grid of every NeroAgriculture block (all faces visible) on a Greenhouse-Frame floor,
 * plus a set of live exhibits — a growing resource crop on a powered bed, the fabrication chain, a formed
 * greenhouse, a formed crop tower, the life-support/biofuel machines, the genetics pair, and a terraforming
 * controller — each fed and powered (a Core Creative Battery neighbour + a pre-charged buffer) with a
 * floating label. {@code /neroagriculture gallery clear} wipes that footprint so a rebuild never stacks.
 *
 * <p><b>Privacy (POPIA/GDPR):</b> the command acts at the player's position and records nothing — no
 * positions, names or UUIDs are stored anywhere.
 */
public final class AgricultureGallery {
    private static final int SPACING = 3;
    private static final int FLOAT_ABOVE = 3;
    private static final int EXHIBIT_STEP = 5;
    private static final Identifier SHOWCASE_MATERIAL = Identifier.parse("c:coal"); // Territe, ungated

    private AgricultureGallery() { }

    public static int build(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("The gallery must be built by a player."));
            return 0;
        }
        if (!player.getAbilities().instabuild) {
            source.sendFailure(Component.literal("The gallery is a creative-only showcase."));
            return 0;
        }
        ServerLevel level = player.level();
        BlockPos origin = player.blockPosition();
        int fy = origin.getY();
        BlockState floor = ModBlocks.GREENHOUSE_FRAME.get().defaultBlockState();

        // BLOCK GRID (EAST): every block registered under this namespace, floating two blocks above a frame.
        List<Block> blocks = new ArrayList<>();
        for (Block block : BuiltInRegistries.BLOCK) {
            if (NeroAgricultureCommon.MOD_ID.equals(BuiltInRegistries.BLOCK.getKey(block).getNamespace())) {
                blocks.add(block);
            }
        }
        int cols = (int) Math.ceil(Math.sqrt(Math.max(1, blocks.size())));
        int gx = origin.getX() + 22;
        int gz = origin.getZ() - 6;
        int rows = (int) Math.ceil(blocks.size() / (double) cols);
        fillFloor(level, gx - 1, gz - 1, gx + cols * SPACING, gz + rows * SPACING, fy, floor);
        for (int i = 0; i < blocks.size(); i++) {
            level.setBlockAndUpdate(new BlockPos(gx + (i % cols) * SPACING, fy + FLOAT_ABOVE, gz + (i / cols) * SPACING),
                    blocks.get(i).defaultBlockState());
        }

        // GROWING CROP (centre): an Forgite Grow Bed, powered, with a mid-growth resource crop on top.
        BlockPos bedPos = new BlockPos(origin.getX() + 4, fy + 1, origin.getZ() - 5);
        fillFloor(level, bedPos.getX() - 1, bedPos.getZ() - 1, bedPos.getX() + 1, bedPos.getZ() + 1, fy, floor);
        level.setBlockAndUpdate(bedPos, ModBlocks.FORGITE_GROW_BED.get().defaultBlockState());
        battery(level, bedPos.below());
        charge(level, bedPos);
        BlockPos cropPos = bedPos.above();
        level.setBlockAndUpdate(cropPos, ModBlocks.RESOURCE_CROP.get().defaultBlockState()
                .setValue(ResourceCropBlock.AGE, 5));
        if (level.getBlockEntity(cropPos) instanceof ResourceCropBlockEntity crop) {
            crop.setVariant(new CropVariantState(CropVariantState.CURRENT_FORMAT, SHOWCASE_MATERIAL, FragmentTier.FORGITE, 3));
        }
        label(level, cropPos.above(2), "Grow Bed — a resource crop mid-growth");

        // FABRICATION STRIP (SOUTH): the four fabrication machines, powered and fed.
        int px = origin.getX() - 8;
        int pz = origin.getZ() + 24;
        fillFloor(level, px - 1, pz - 1, px + 3 * EXHIBIT_STEP + 1, pz + 1, fy, floor);
        liveMachine(level, new BlockPos(px, fy + 1, pz), ModBlocks.FRAGMENT_EXTRACTOR.get(),
                new ItemStack(Items.COAL, 64), 0, "Fragment Extractor — sample → fragment");
        liveMachine(level, new BlockPos(px + EXHIBIT_STEP, fy + 1, pz), ModBlocks.FRAGMENT_INFUSER.get(),
                new ItemStack(ModItems.TERRITE_FRAGMENT.get(), 64), 0, "Fragment Infuser — condensing fragment");
        liveMachine(level, new BlockPos(px + EXHIBIT_STEP * 2, fy + 1, pz), ModBlocks.SEED_SYNTHESIZER.get(),
                new ItemStack(Items.COAL, 64), 0, "Seed Synthesizer — fabricating a seed");
        liveMachine(level, new BlockPos(px + EXHIBIT_STEP * 3, fy + 1, pz), ModBlocks.SEED_RESEARCH_BENCH.get(),
                new ItemStack(Items.WHEAT, 64), 0, "Research Bench — discovering a seed");

        // AUTOMATION + LIFE SUPPORT (SW): planter, bioreactor, biofuel converter, fertiliser processor.
        int wx = origin.getX() - 26;
        int wz = origin.getZ() + 12;
        fillFloor(level, wx - 1, wz - 1, wx + 3 * EXHIBIT_STEP + 1, wz + 1, fy, floor);
        liveMachine(level, new BlockPos(wx, fy + 1, wz), ModBlocks.PLANTER.get(),
                resourceSeed(), 0, "Planter — sowing an area of beds");
        liveMachine(level, new BlockPos(wx + EXHIBIT_STEP, fy + 1, wz), ModBlocks.OXYGEN_PLANT.get(),
                new ItemStack(ModItems.BIOMASS.get(), 64), 0, "Bioreactor — biomass → nutrient");
        liveMachine(level, new BlockPos(wx + EXHIBIT_STEP * 2, fy + 1, wz), ModBlocks.BIOFUEL_CONVERTER.get(),
                new ItemStack(ModItems.BIOMASS.get(), 64), 0, "Biofuel Converter — farm surplus → fuel");
        liveMachine(level, new BlockPos(wx + EXHIBIT_STEP * 3, fy + 1, wz), ModBlocks.FERTILISER_PROCESSOR.get(),
                new ItemStack(ModItems.BIOMASS.get(), 64), 0, "Fertiliser Processor — making fertiliser");

        // GENETICS + POLLINATION (W): the station splicing a seed, the beacon boosting crossing.
        int ax = origin.getX() - 26;
        int az = origin.getZ() - 4;
        fillFloor(level, ax - 1, az - 1, ax + EXHIBIT_STEP + 1, az + 1, fy, floor);
        liveMachine(level, new BlockPos(ax, fy + 1, az), ModBlocks.GENETICS_STATION.get(),
                resourceSeed(), 0, "Genetics Station — splicing traits");
        BlockPos beaconPos = new BlockPos(ax + EXHIBIT_STEP, fy + 1, az);
        level.setBlockAndUpdate(beaconPos, ModBlocks.POLLINATION_BEACON.get().defaultBlockState());
        battery(level, beaconPos.below());
        charge(level, beaconPos);
        label(level, beaconPos.above(2), "Pollination Beacon — boosting crossing");

        // TERRAFORMING (near centre): a powered controller ready to convert a hostile region.
        BlockPos terraPos = new BlockPos(origin.getX() + 5, fy + 1, origin.getZ() + 5);
        fillFloor(level, terraPos.getX() - 1, terraPos.getZ() - 1, terraPos.getX() + 1, terraPos.getZ() + 1, fy, floor);
        level.setBlockAndUpdate(terraPos, ModBlocks.TERRAFORMING_CONTROLLER.get().defaultBlockState());
        battery(level, terraPos.below());
        charge(level, terraPos);
        label(level, terraPos.above(2), "Terraforming Controller — makes a region habitable");

        // FORMED EXHIBITS (NORTH): a sealed greenhouse and a crop tower.
        buildGreenhouse(level, floor, origin.getX() - 1, origin.getZ() - 27, fy);
        buildCropTower(level, floor, origin.getX() + 12, origin.getZ() - 25, fy);

        // WORKING FARM (SE): the intended per-tier setup — powered + nutrient-fed grow beds, each with a
        // fully grown ore crop ready to right-click harvest (the crop stays planted).
        buildFarm(level, floor, origin.getX() + 12, origin.getZ() + 8, fy);

        int count = blocks.size();
        source.sendSuccess(() -> Component.literal("[NeroAgriculture] Gallery built (" + count
                + " blocks on display). Run /neroagriculture gallery clear here to remove it."), false);
        return Command.SINGLE_SUCCESS;
    }

    public static int clear(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal("The gallery must be cleared by a player."));
            return 0;
        }
        if (!player.getAbilities().instabuild) {
            source.sendFailure(Component.literal("The gallery is a creative-only showcase."));
            return 0;
        }
        ServerLevel level = player.level();
        BlockPos origin = player.blockPosition();
        int oy = origin.getY();
        int minX = origin.getX() - 32;
        int maxX = origin.getX() + 42;
        int minZ = origin.getZ() - 32;
        int maxZ = origin.getZ() + 30;
        int topY = oy + 16;
        BlockState air = Blocks.AIR.defaultBlockState();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        int cleared = 0;
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int y = oy; y <= topY; y++) {
                    cursor.set(x, y, z);
                    if (!level.getBlockState(cursor).isAir()) {
                        level.setBlock(cursor, air, 2);
                        cleared++;
                    }
                }
            }
        }
        AABB box = new AABB(minX, oy - 1, minZ, maxX + 1, topY + 4, maxZ + 1);
        int removed = 0;
        for (Entity entity : level.getEntitiesOfClass(Entity.class, box, e -> !(e instanceof Player))) {
            entity.discard();
            removed++;
        }
        int clearedBlocks = cleared;
        int removedEntities = removed;
        source.sendSuccess(() -> Component.literal("[NeroAgriculture] Gallery cleared (" + clearedBlocks
                + " blocks, " + removedEntities + " labels removed)."), false);
        return Command.SINGLE_SUCCESS;
    }

    // --- helpers -----------------------------------------------------------

    private static void liveMachine(ServerLevel level, BlockPos pos, Block block, ItemStack input, int slot, String label) {
        level.setBlockAndUpdate(pos, block.defaultBlockState());
        battery(level, pos.below());
        charge(level, pos);
        if (level.getBlockEntity(pos) instanceof Container container && slot < container.getContainerSize()) {
            container.setItem(slot, input);
        }
        label(level, pos.above(2), label);
    }

    private static ItemStack resourceSeed() {
        ItemStack seed = new ItemStack(ModItems.RESOURCE_SEED.get(), 16);
        seed.set(ModDataComponents.MATERIAL_VARIANT.get(), MaterialVariant.of(SHOWCASE_MATERIAL, FragmentTier.FORGITE));
        za.co.neroland.neroagriculture.content.MaterialTints.apply(seed, SHOWCASE_MATERIAL);
        return seed;
    }

    private static void battery(ServerLevel level, BlockPos pos) {
        level.setBlockAndUpdate(pos, za.co.neroland.nerolandcore.registry.ModBlocks.CREATIVE_BATTERY.get().defaultBlockState());
    }

    private static void charge(ServerLevel level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof AbstractMachineBlockEntity machine
                && machine.getEnergy() instanceof EnergyBuffer buffer) {
            buffer.setRaw(Integer.MAX_VALUE);
            machine.setChanged();
        }
    }

    private static void fillFloor(ServerLevel level, int x0, int z0, int x1, int z1, int y, BlockState floor) {
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                level.setBlockAndUpdate(new BlockPos(x, y, z), floor);
            }
        }
    }

    private static void buildGreenhouse(ServerLevel level, BlockState floor, int bx, int bz, int fy) {
        int size = 5;
        fillFloor(level, bx - 1, bz - 1, bx + size, bz + size, fy, floor);
        BlockState frame = ModBlocks.GREENHOUSE_FRAME.get().defaultBlockState();
        BlockState glass = ModBlocks.GREENHOUSE_GLASS.get().defaultBlockState();
        int y0 = fy + 1;
        BlockPos controllerPos = new BlockPos(bx + size / 2, y0 + size / 2, bz + size - 1);
        for (int dx = 0; dx < size; dx++) {
            for (int dy = 0; dy < size; dy++) {
                for (int dz = 0; dz < size; dz++) {
                    BlockPos pos = new BlockPos(bx + dx, y0 + dy, bz + dz);
                    boolean surface = dx == 0 || dx == size - 1 || dy == 0 || dy == size - 1 || dz == 0 || dz == size - 1;
                    if (pos.equals(controllerPos)) continue;
                    if (!surface) level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
                    else level.setBlockAndUpdate(pos, dy == size / 2 ? glass : frame);
                }
            }
        }
        level.setBlockAndUpdate(controllerPos, ModBlocks.GREENHOUSE_CONTROLLER.get().defaultBlockState());
        battery(level, controllerPos.south());
        charge(level, controllerPos);
        label(level, new BlockPos(bx + size / 2, fy + size + 2, bz + size / 2), "Greenhouse — a sealed, powered dome");
    }

    private static void buildCropTower(ServerLevel level, BlockState floor, int bx, int bz, int fy) {
        fillFloor(level, bx - 1, bz - 1, bx + 1, bz + 1, fy, floor);
        BlockPos controllerPos = new BlockPos(bx, fy + 1, bz);
        level.setBlockAndUpdate(controllerPos, ModBlocks.CROP_TOWER_CONTROLLER.get().defaultBlockState());
        battery(level, controllerPos.north());
        charge(level, controllerPos);
        for (int dy = 1; dy <= 5; dy++) {
            level.setBlockAndUpdate(controllerPos.above(dy), ModBlocks.CROP_TOWER_FRAME.get().defaultBlockState());
        }
        if (level.getBlockEntity(controllerPos) instanceof Container container) {
            container.setItem(0, resourceSeed());
        }
        label(level, controllerPos.above(7), "Crop Tower — a formed vertical farm");
    }

    /**
     * A working demonstration farm: a 3×2 grid of tier grow beds, each powered + nutrient-fed with a fully
     * grown ore crop on top, flanked by a Planter and Harvester. Every crop is at {@code MAX_AGE}, so
     * right-clicking one harvests its fragment and replants the seed (age resets to 0). Higher-tier ores still
     * require their Core progression gate to be open to harvest — that is intended game behaviour.
     */
    private static void buildFarm(ServerLevel level, BlockState floor, int bx, int bz, int fy) {
        int cols = 3;
        fillFloor(level, bx - 2, bz - 1, bx + cols * 3 + 1, bz + 2 * 3, fy, floor);
        label(level, new BlockPos(bx + 3, fy + 4, bz - 1),
                "Farm — power + nutrient each bed; right-click a grown crop to harvest (it stays planted)");
        Object[][] plots = {
            {ModBlocks.TERRITE_GROW_BED.get(), "c:coal", FragmentTier.TERRITE, "Coal — Territe (ungated)"},
            {ModBlocks.FORGITE_GROW_BED.get(), "c:iron", FragmentTier.FORGITE, "Iron — Forgite"},
            {ModBlocks.FORGITE_GROW_BED.get(), "c:copper", FragmentTier.FORGITE, "Copper — Forgite"},
            {ModBlocks.FORGITE_GROW_BED.get(), "c:gold", FragmentTier.FORGITE, "Gold — Forgite"},
            {ModBlocks.ORBITE_GROW_BED.get(), "c:diamond", FragmentTier.ORBITE, "Diamond — Orbite"},
            {ModBlocks.ORBITE_GROW_BED.get(), "c:emerald", FragmentTier.ORBITE, "Emerald — Orbite"},
        };
        for (int i = 0; i < plots.length; i++) {
            BlockPos bedPos = new BlockPos(bx + (i % cols) * 3, fy + 1, bz + (i / cols) * 3);
            plantBed(level, bedPos, (Block) plots[i][0], Identifier.parse((String) plots[i][1]),
                    (FragmentTier) plots[i][2], (String) plots[i][3]);
        }
        liveMachine(level, new BlockPos(bx - 2, fy + 1, bz + 1), ModBlocks.PLANTER.get(),
                resourceSeed(), 0, "Planter — auto-sows the beds");
        liveMachine(level, new BlockPos(bx + cols * 3 + 1, fy + 1, bz + 1), ModBlocks.HARVESTER.get(),
                ItemStack.EMPTY, 0, "Harvester — auto-reaps grown crops");
    }

    private static void plantBed(ServerLevel level, BlockPos bedPos, Block bed, Identifier material,
            FragmentTier tier, String label) {
        level.setBlockAndUpdate(bedPos, bed.defaultBlockState());
        battery(level, bedPos.below());
        charge(level, bedPos);
        if (level.getBlockEntity(bedPos) instanceof GrowBedBlockEntity growBed
                && growBed.getFluid() instanceof FluidBuffer fluid) {
            fluid.setRaw(ModFluids.NUTRIENT.get(), 100_000);
            growBed.setChanged();
        }
        BlockPos cropPos = bedPos.above();
        level.setBlockAndUpdate(cropPos, ModBlocks.RESOURCE_CROP.get().defaultBlockState()
                .setValue(ResourceCropBlock.AGE, ResourceCropBlock.MAX_AGE));
        if (level.getBlockEntity(cropPos) instanceof ResourceCropBlockEntity crop) {
            crop.setVariant(new CropVariantState(CropVariantState.CURRENT_FORMAT, material, tier, 0));
        }
        label(level, cropPos.above(1), label);
    }

    private static void label(ServerLevel level, BlockPos pos, String text) {
        ArmorStand stand = new ArmorStand(level, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        stand.setCustomName(Component.literal(text));
        stand.setCustomNameVisible(true);
        stand.setInvisible(true);
        stand.setNoGravity(true);
        stand.setInvulnerable(true);
        level.addFreshEntity(stand);
    }
}
