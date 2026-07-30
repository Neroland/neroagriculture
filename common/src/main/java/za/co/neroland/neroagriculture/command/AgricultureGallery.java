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
 * floating label. {@code /neroagriculture gallery clear} removes exactly the blocks and label stands the
 * gallery itself placed (recorded per dimension for the session) so a rebuild never stacks — and never
 * touches anything a player built inside the same footprint.
 *
 * <p><b>Privacy (POPIA/GDPR):</b> the command acts at the player's position and stores no personal data
 * — the only record kept (in memory, for the session) is the set of block positions the gallery painted,
 * so {@code clear} can remove them; no names or UUIDs are stored anywhere.
 */
public final class AgricultureGallery {
    private static final int SPACING = 3;
    private static final int FLOAT_ABOVE = 3;
    private static final int EXHIBIT_STEP = 5;
    private static final Identifier SHOWCASE_MATERIAL = Identifier.parse("c:coal"); // Territe, ungated
    /** Scoreboard-style entity tag on the gallery's own label stands, so clear() removes only those. */
    private static final String LABEL_TAG = "neroagriculture_gallery";

    /**
     * Session-scoped record of every position the gallery painted, per dimension, so {@code clear}
     * removes exactly what {@code build} placed and never a player's pre-existing blocks. Block
     * positions only — no names, UUIDs or player data of any kind (POPIA/GDPR).
     */
    private static final java.util.Map<Identifier, java.util.Set<BlockPos>> PLACED =
            new java.util.concurrent.ConcurrentHashMap<>();

    private AgricultureGallery() { }

    /** Every gallery placement goes through here so the footprint is recorded for {@link #clear}. */
    private static void place(ServerLevel level, BlockPos pos, BlockState state) {
        level.setBlockAndUpdate(pos, state);
        PLACED.computeIfAbsent(level.dimension().identifier(),
                key -> java.util.concurrent.ConcurrentHashMap.newKeySet()).add(pos.immutable());
    }

    /** Server-stop hook (see {@code lifecycle.ServerStateReset}): the record must not outlive the world. */
    public static void clearRecords() { PLACED.clear(); }

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

        // BLOCK GRID (EAST): every block registered under this namespace, floating two blocks above a
        // frame. Fluids are excluded — placing a liquid source here floods the gallery. Doors are excluded
        // too: a floating lower half has no support or upper half, so the Greenhouse Door is demonstrated
        // in the greenhouse exhibit's wall instead.
        List<Block> blocks = new ArrayList<>();
        for (Block block : BuiltInRegistries.BLOCK) {
            if (NeroAgricultureCommon.MOD_ID.equals(BuiltInRegistries.BLOCK.getKey(block).getNamespace())
                    && !(block instanceof net.minecraft.world.level.block.LiquidBlock)
                    && !(block instanceof net.minecraft.world.level.block.DoorBlock)) {
                blocks.add(block);
            }
        }
        int cols = (int) Math.ceil(Math.sqrt(Math.max(1, blocks.size())));
        int gx = origin.getX() + 22;
        int gz = origin.getZ() - 6;
        int rows = (int) Math.ceil(blocks.size() / (double) cols);
        fillFloor(level, gx - 1, gz - 1, gx + cols * SPACING, gz + rows * SPACING, fy, floor);
        for (int i = 0; i < blocks.size(); i++) {
            place(level, new BlockPos(gx + (i % cols) * SPACING, fy + FLOAT_ABOVE, gz + (i / cols) * SPACING),
                    blocks.get(i).defaultBlockState());
        }

        // GROWING CROP (centre): an Forgite Grow Bed, powered, with a mid-growth resource crop on top.
        BlockPos bedPos = new BlockPos(origin.getX() + 4, fy + 1, origin.getZ() - 5);
        fillFloor(level, bedPos.getX() - 1, bedPos.getZ() - 1, bedPos.getX() + 1, bedPos.getZ() + 1, fy, floor);
        place(level, bedPos, ModBlocks.FORGITE_GROW_BED.get().defaultBlockState());
        battery(level, bedPos.below());
        charge(level, bedPos);
        BlockPos cropPos = bedPos.above();
        place(level, cropPos, ModBlocks.RESOURCE_CROP.get().defaultBlockState()
                .setValue(ResourceCropBlock.AGE, 5));
        if (level.getBlockEntity(cropPos) instanceof ResourceCropBlockEntity crop) {
            crop.setVariant(new CropVariantState(CropVariantState.CURRENT_FORMAT, SHOWCASE_MATERIAL, FragmentTier.FORGITE, 3));
        }
        label(level, cropPos.above(2), "Grow Bed — a resource crop mid-growth");

        // FABRICATION STRIP (SOUTH): the fabrication chain end to end, powered and fed.
        int px = origin.getX() - 8;
        int pz = origin.getZ() + 24;
        fillFloor(level, px - 1, pz - 1, px + 4 * EXHIBIT_STEP + 1, pz + 1, fy, floor);
        liveMachine(level, new BlockPos(px, fy + 1, pz), ModBlocks.FRAGMENT_EXTRACTOR.get(),
                new ItemStack(Items.COAL, 64), 0, "1. Fragment Extractor — sample → Territe Fragment");
        liveMachine(level, new BlockPos(px + EXHIBIT_STEP, fy + 1, pz), ModBlocks.FRAGMENT_INFUSER.get(),
                new ItemStack(ModItems.TERRITE_FRAGMENT.get(), 64), 0,
                "2. Fragment Infuser — 4x Territe → Forgite (the ladder)");
        BlockPos fusionPos = new BlockPos(px + EXHIBIT_STEP * 2, fy + 1, pz);
        liveMachine(level, fusionPos, ModBlocks.FRAGMENT_INFUSER.get(),
                resourceFragment(Identifier.parse("c:iron"), FragmentTier.FORGITE, 64), 0,
                "3. Fusion — Iron Fragments + Redstone → Nero Alloy Seed");
        if (level.getBlockEntity(fusionPos) instanceof Container fusion && fusion.getContainerSize() > 1) {
            fusion.setItem(1, new ItemStack(Items.REDSTONE, 64));
        }
        BlockPos synthPos = new BlockPos(px + EXHIBIT_STEP * 3, fy + 1, pz);
        liveMachine(level, synthPos, ModBlocks.SEED_SYNTHESIZER.get(),
                new ItemStack(Items.COAL, 64), 0, "4. Seed Synthesizer — resource + fragments + Prospora → seed");
        if (level.getBlockEntity(synthPos) instanceof Container synth && synth.getContainerSize() > 2) {
            synth.setItem(1, new ItemStack(ModItems.TERRITE_FRAGMENT.get(), 64));
            synth.setItem(2, new ItemStack(ModItems.PROSPORA_SEED.get(), 16));
        }
        liveMachine(level, new BlockPos(px + EXHIBIT_STEP * 4, fy + 1, pz), ModBlocks.SEED_RESEARCH_BENCH.get(),
                new ItemStack(Items.WHEAT, 64), 0, "5. Research Bench — optional seed discovery");

        // AUTOMATION + LIFE SUPPORT (SW): bioreactor, biofuel converter, fertiliser processor + applicator.
        int wx = origin.getX() - 26;
        int wz = origin.getZ() + 12;
        fillFloor(level, wx - 1, wz - 1, wx + 3 * EXHIBIT_STEP + 1, wz + 1, fy, floor);
        liveMachine(level, new BlockPos(wx, fy + 1, wz), ModBlocks.OXYGEN_PLANT.get(),
                new ItemStack(ModItems.BIOMASS.get(), 64), 0, "Bioreactor — biomass → nutrient");
        liveMachine(level, new BlockPos(wx + EXHIBIT_STEP, fy + 1, wz), ModBlocks.BIOFUEL_CONVERTER.get(),
                new ItemStack(ModItems.BIOMASS.get(), 64), 0, "Biofuel Converter — farm surplus → fuel");
        liveMachine(level, new BlockPos(wx + EXHIBIT_STEP * 2, fy + 1, wz), ModBlocks.FERTILISER_PROCESSOR.get(),
                new ItemStack(ModItems.BIOMASS.get(), 64), 0, "Fertiliser Processor — making fertiliser");
        liveMachine(level, new BlockPos(wx + EXHIBIT_STEP * 3, fy + 1, wz), ModBlocks.FERTILISER_APPLICATOR.get(),
                new ItemStack(ModItems.FERTILISER.get(), 64), 0, "Fertiliser Applicator — auto-doses beds");

        // PROSPORA PLOT (SW corner): the base crop on plain farmland — the standalone ladder entry.
        int fx2 = wx;
        int fz2 = wz + 5;
        fillFloor(level, fx2 - 1, fz2 - 1, fx2 + 4, fz2 + 1, fy, floor);
        for (int i = 0; i < 4; i++) {
            BlockPos farm = new BlockPos(fx2 + i, fy + 1, fz2);
            place(level, farm, Blocks.FARMLAND.defaultBlockState());
            place(level, farm.above(), ModBlocks.PROSPORA_CROP.get().defaultBlockState()
                    .setValue(net.minecraft.world.level.block.CropBlock.AGE, Math.min(7, 1 + i * 2)));
        }
        label(level, new BlockPos(fx2 + 2, fy + 4, fz2),
                "Prospora Crop — grows on farmland, drops Territe Fragments");

        // GENETICS + POLLINATION (W): the station splicing a seed, the beacon boosting crossing.
        int ax = origin.getX() - 26;
        int az = origin.getZ() - 4;
        fillFloor(level, ax - 1, az - 1, ax + EXHIBIT_STEP + 1, az + 1, fy, floor);
        liveMachine(level, new BlockPos(ax, fy + 1, az), ModBlocks.GENETICS_STATION.get(),
                resourceSeed(), 0, "Genetics Station — splicing traits");
        BlockPos beaconPos = new BlockPos(ax + EXHIBIT_STEP, fy + 1, az);
        place(level, beaconPos, ModBlocks.POLLINATION_BEACON.get().defaultBlockState());
        battery(level, beaconPos.below());
        charge(level, beaconPos);
        label(level, beaconPos.above(2), "Pollination Beacon — boosting crossing");

        // TERRAFORMING (near centre): a powered controller ready to convert a hostile region.
        BlockPos terraPos = new BlockPos(origin.getX() + 5, fy + 1, origin.getZ() + 5);
        fillFloor(level, terraPos.getX() - 1, terraPos.getZ() - 1, terraPos.getX() + 1, terraPos.getZ() + 1, fy, floor);
        place(level, terraPos, ModBlocks.TERRAFORMING_CONTROLLER.get().defaultBlockState());
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

    /**
     * Removes exactly the blocks {@link #build} recorded for this dimension (never a blanket box wipe,
     * which used to delete any pre-existing build inside the footprint) and only the gallery's own
     * tagged label stands. Uses flag 3 (block update + client sync) so neighbours resettle properly.
     */
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
        java.util.Set<BlockPos> placed = PLACED.remove(level.dimension().identifier());
        if (placed == null || placed.isEmpty()) {
            source.sendFailure(Component.literal("[NeroAgriculture] No gallery build recorded in this dimension "
                    + "this session — nothing cleared. The command only removes blocks the gallery itself placed."));
            return 0;
        }
        BlockState air = Blocks.AIR.defaultBlockState();
        int cleared = 0;
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;
        for (BlockPos pos : placed) {
            minX = Math.min(minX, pos.getX());
            minY = Math.min(minY, pos.getY());
            minZ = Math.min(minZ, pos.getZ());
            maxX = Math.max(maxX, pos.getX());
            maxY = Math.max(maxY, pos.getY());
            maxZ = Math.max(maxZ, pos.getZ());
            if (!level.getBlockState(pos).isAir()) {
                level.setBlock(pos, air, 3);
                cleared++;
            }
        }
        AABB box = new AABB(minX - 1, minY - 1, minZ - 1, maxX + 2, maxY + 5, maxZ + 2);
        int removed = 0;
        for (Entity entity : level.getEntitiesOfClass(ArmorStand.class, box,
                e -> e.entityTags().contains(LABEL_TAG))) {
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
        place(level, pos, block.defaultBlockState());
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

    private static ItemStack resourceSeed(Identifier material, FragmentTier tier) {
        ItemStack seed = new ItemStack(ModItems.RESOURCE_SEED.get(), 16);
        seed.set(ModDataComponents.MATERIAL_VARIANT.get(), MaterialVariant.of(material, tier));
        za.co.neroland.neroagriculture.content.MaterialTints.apply(seed, material);
        return seed;
    }

    private static ItemStack resourceFragment(Identifier material, FragmentTier tier, int count) {
        ItemStack fragment = new ItemStack(ModItems.RESOURCE_FRAGMENT.get(), count);
        fragment.set(ModDataComponents.MATERIAL_VARIANT.get(), MaterialVariant.of(material, tier));
        za.co.neroland.neroagriculture.content.MaterialTints.apply(fragment, material);
        return fragment;
    }

    private static void battery(ServerLevel level, BlockPos pos) {
        place(level, pos, za.co.neroland.nerolandcore.registry.ModBlocks.CREATIVE_BATTERY.get().defaultBlockState());
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
                place(level, new BlockPos(x, y, z), floor);
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
                    if (!surface) place(level, pos, Blocks.AIR.defaultBlockState());
                    else place(level, pos, dy == size / 2 ? glass : frame);
                }
            }
        }
        place(level, controllerPos, ModBlocks.GREENHOUSE_CONTROLLER.get().defaultBlockState());
        // Power from BELOW, replacing a wall frame block (the battery is solid, so the seal holds) —
        // a battery in front of the controller used to block its only clickable exterior face.
        battery(level, controllerPos.below());
        charge(level, controllerPos);
        // Walk-in airlock: a Greenhouse Door in the front wall beside the controller column. Both halves
        // count as sealing shell whether open or closed, so visitors can step inside without breaching.
        BlockPos doorPos = new BlockPos(bx + 1, y0 + 1, bz + size - 1);
        BlockState door = ModBlocks.GREENHOUSE_DOOR.get().defaultBlockState()
                .setValue(net.minecraft.world.level.block.DoorBlock.FACING, net.minecraft.core.Direction.SOUTH);
        place(level, doorPos, door.setValue(net.minecraft.world.level.block.DoorBlock.HALF,
                net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER));
        place(level, doorPos.above(), door.setValue(net.minecraft.world.level.block.DoorBlock.HALF,
                net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER));
        label(level, new BlockPos(bx + size / 2, fy + size + 2, bz + size / 2),
                "Greenhouse — a sealed, powered dome (walk in through the door)");
    }

    private static void buildCropTower(ServerLevel level, BlockState floor, int bx, int bz, int fy) {
        fillFloor(level, bx - 1, bz - 1, bx + 1, bz + 1, fy, floor);
        BlockPos controllerPos = new BlockPos(bx, fy + 1, bz);
        place(level, controllerPos, ModBlocks.CROP_TOWER_CONTROLLER.get().defaultBlockState());
        battery(level, controllerPos.north());
        charge(level, controllerPos);
        for (int dy = 1; dy <= 5; dy++) {
            place(level, controllerPos.above(dy), ModBlocks.CROP_TOWER_FRAME.get().defaultBlockState());
        }
        if (level.getBlockEntity(controllerPos) instanceof Container container) {
            container.setItem(0, resourceSeed());
        }
        label(level, controllerPos.above(7), "Crop Tower — a formed vertical farm");
    }

    /**
     * A working demonstration farm: a 3×2 grid of tier grow beds, each powered + nutrient-fed with a fully
     * grown ore crop on top AND a stack of matching seeds in its seed slot — so the beds demonstrate the
     * full hopping-pot loop (auto-harvest into their own output slots, auto-replant from the seed slot).
     * Flanked by a Planter and a Harvester with their working-area holograms switched on.
     */
    private static void buildFarm(ServerLevel level, BlockState floor, int bx, int bz, int fy) {
        int cols = 3;
        fillFloor(level, bx - 2, bz - 1, bx + cols * 3 + 1, bz + 2 * 3, fy, floor);
        label(level, new BlockPos(bx + 3, fy + 4, bz - 1),
                "Farm — beds auto-harvest into their own slots and replant from their seed slot");
        // The Territe bed is a deliberately passive plain block (no block entity, no seed/output slots
        // — see GrowBedBlock), so it cannot demonstrate the auto-harvest/replant loop this farm is
        // labelled with; the Territe crop sits on a Forgite bed instead (beds accept lower-tier crops).
        Object[][] plots = {
            {ModBlocks.FORGITE_GROW_BED.get(), "c:coal", FragmentTier.TERRITE, "Coal — Territe (on a Forgite bed)"},
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
        BlockPos planterPos = new BlockPos(bx - 2, fy + 1, bz + 1);
        liveMachine(level, planterPos, ModBlocks.PLANTER.get(),
                resourceSeed(), 0, "Planter — auto-sows a 7x7 area (hologram on)");
        showArea(level, planterPos);
        BlockPos harvesterPos = new BlockPos(bx + cols * 3 + 1, fy + 1, bz + 1);
        liveMachine(level, harvesterPos, ModBlocks.HARVESTER.get(),
                ItemStack.EMPTY, 0, "Harvester — auto-reaps a 7x7 area (hologram on)");
        showArea(level, harvesterPos);
    }

    private static void showArea(ServerLevel level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof za.co.neroland.neroagriculture.automation.AreaMachineBlockEntity machine) {
            machine.toggleShowArea();
        }
    }

    private static void plantBed(ServerLevel level, BlockPos bedPos, Block bed, Identifier material,
            FragmentTier tier, String label) {
        place(level, bedPos, bed.defaultBlockState());
        battery(level, bedPos.below());
        charge(level, bedPos);
        if (level.getBlockEntity(bedPos) instanceof GrowBedBlockEntity growBed) {
            if (growBed.getFluid() instanceof FluidBuffer fluid) {
                fluid.setRaw(ModFluids.NUTRIENT.get(), 100_000);
            }
            growBed.setItem(GrowBedBlockEntity.SEED_SLOT, resourceSeed(material, tier));
            growBed.setChanged();
        }
        BlockPos cropPos = bedPos.above();
        place(level, cropPos, ModBlocks.RESOURCE_CROP.get().defaultBlockState()
                .setValue(ResourceCropBlock.AGE, ResourceCropBlock.MAX_AGE));
        if (level.getBlockEntity(cropPos) instanceof ResourceCropBlockEntity crop) {
            crop.setVariant(new CropVariantState(CropVariantState.CURRENT_FORMAT, material, tier, 0));
        }
        label(level, cropPos.above(1), label);
    }

    private static void label(ServerLevel level, BlockPos pos, String text) {
        ArmorStand stand = new ArmorStand(level, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        stand.addTag(LABEL_TAG); // clear() removes only stands carrying this tag
        stand.setCustomName(Component.literal(text));
        stand.setCustomNameVisible(true);
        stand.setInvisible(true);
        stand.setNoGravity(true);
        stand.setInvulnerable(true);
        level.addFreshEntity(stand);
    }
}
