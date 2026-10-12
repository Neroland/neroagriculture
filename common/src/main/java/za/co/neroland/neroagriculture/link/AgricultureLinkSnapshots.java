package za.co.neroland.neroagriculture.link;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerolandcore.link.LinkSnapshotProvider;
import za.co.neroland.nerolandcore.progression.MaterialMilestoneDefinitions;
import za.co.neroland.nerolandcore.progression.MaterialMilestones;
import za.co.neroland.nerolandcore.progression.ProgressionGates;

import za.co.neroland.neroagriculture.NeroAgricultureCommon;
import za.co.neroland.neroagriculture.automation.AreaMachineBlockEntity;
import za.co.neroland.neroagriculture.catalog.MaterialCatalog;
import za.co.neroland.neroagriculture.catalog.ResolvedMaterial;
import za.co.neroland.neroagriculture.config.AgricultureConfig;
import za.co.neroland.neroagriculture.content.FragmentTier;
import za.co.neroland.neroagriculture.crop.GrowBedBlockEntity;
import za.co.neroland.neroagriculture.crop.GrowthRules;
import za.co.neroland.neroagriculture.crop.ResourceCropBlock;
import za.co.neroland.neroagriculture.crop.ResourceCropBlockEntity;
import za.co.neroland.neroagriculture.crop.SpeciesCropBlock;
import za.co.neroland.neroagriculture.crop.SpeciesCropBlockEntity;
import za.co.neroland.neroagriculture.greenhouse.GreenhouseControllerBlockEntity;
import za.co.neroland.neroagriculture.greenhouse.GreenhouseIndex;
import za.co.neroland.neroagriculture.machine.FoundationMachineBlockEntity;
import za.co.neroland.neroagriculture.progression.AgricultureGates;
import za.co.neroland.neroagriculture.terraforming.TerraformingControllerBlockEntity;

/**
 * The read half of the link module: the state of the farm the requesting player actually owns.
 *
 * <h2>Sections</h2>
 *
 * <ul>
 *   <li>{@code crops} — one row per grow bed this player owns: tier, power and nutrient, what is
 *       planted above it, how far along it is, and what (if anything) is stopping it;</li>
 *   <li>{@code machines} — the player's fabrication machines, area machines and terraforming
 *       controllers, with progress and the reason each is idle;</li>
 *   <li>{@code greenhouse} — the greenhouses that enclose the player's own grow beds: seal state,
 *       volume, crop count, oxygen and where the leak is;</li>
 *   <li>{@code fragments} — progress up the Cosmic-ascent ladder: per tier, how many catalog materials
 *       the player has discovered out of how many exist, and whether that tier's gate is open.</li>
 * </ul>
 *
 * <p>Any other section name yields an empty object, as Core's contract prescribes.
 *
 * <h2>Privacy (POPIA/GDPR)</h2>
 *
 * <p>{@link AgricultureLinkAccess#ownedMachines} is the whole visibility rule for the first three
 * sections and it runs first in each: a request sees the machines its own UUID owns and nothing else.
 * The {@code fragments} section is keyed by the same UUID through Core's own per-player milestone store.
 *
 * <p><b>The greenhouse section is scoped indirectly, on purpose.</b> A greenhouse controller carries no
 * owner, so it is never reported for its own sake — only when it encloses a grow bed the requesting
 * player owns. The authorisation comes from the bed. Within such a greenhouse the crop count is an
 * aggregate: it counts crops, it does not say whose.
 *
 * <p><b>No crop tower section.</b> The tower controller has no owner field at all and its in-world gate
 * checks fall back to the nearest player, which is not a permission model. See
 * {@link AgricultureLinkModule} for the full reasoning.
 *
 * <p><b>Read-only and bounded.</b> Nothing here mutates anything and no section loads a chunk: the
 * machine set is the loaded, tracked one, and the greenhouse lookup is an in-memory index consulted
 * behind an {@code isLoaded} check. The per-request work is proportional to the number of machines the
 * requesting player owns. The bridge governs how often it is called and caches the result.
 *
 * <p>Server thread only.
 */
public final class AgricultureLinkSnapshots implements LinkSnapshotProvider {

    private static final List<String> SECTIONS = List.of(
            AgricultureLinkModule.SECTION_CROPS,
            AgricultureLinkModule.SECTION_MACHINES,
            AgricultureLinkModule.SECTION_GREENHOUSE,
            AgricultureLinkModule.SECTION_FRAGMENTS);

    @Override
    public String moduleId() {
        return AgricultureLinkModule.MODULE_ID;
    }

    @Override
    public int schemaVersion() {
        return AgricultureLinkModule.SCHEMA_VERSION;
    }

    @Override
    public List<String> sections() {
        return SECTIONS;
    }

    @Override
    public JsonObject snapshot(UUID playerId, String section, Map<String, String> params) {
        if (playerId == null || section == null) {
            return new JsonObject();
        }
        MinecraftServer server = AgricultureLinkAccess.server();
        if (server == null || !AgricultureConfig.LINK_MODULE_ENABLED.get()) {
            return new JsonObject();
        }
        try {
            return switch (section) {
                case AgricultureLinkModule.SECTION_CROPS -> crops(playerId);
                case AgricultureLinkModule.SECTION_MACHINES -> machines(playerId);
                case AgricultureLinkModule.SECTION_GREENHOUSE -> greenhouses(playerId);
                case AgricultureLinkModule.SECTION_FRAGMENTS -> fragments(server, playerId);
                default -> new JsonObject();
            };
        } catch (RuntimeException e) {
            // Never who it was for (POPIA/GDPR), and never thrown at the bridge.
            NeroAgricultureCommon.LOGGER.warn(
                    "[NeroAgriculture] Building the NeroLink '{}' snapshot failed.", section, e);
            return new JsonObject();
        }
    }

    // --- crops -----------------------------------------------------------------

    private JsonObject crops(UUID playerId) {
        JsonObject out = AgricultureLinkAccess.envelope();
        JsonArray beds = new JsonArray();
        for (GrowBedBlockEntity bed : AgricultureLinkAccess.ownedMachines(playerId, GrowBedBlockEntity.class)) {
            if (!(bed.getLevel() instanceof ServerLevel level)) {
                continue;
            }
            BlockPos pos = bed.getBlockPos();
            JsonObject row = new JsonObject();
            row.addProperty("id", AgricultureLinkAccess.machineKey(bed));
            row.addProperty("dimension", AgricultureLinkAccess.dimensionOf(bed));
            row.add("position", AgricultureLinkAccess.position(pos));
            row.addProperty("tier", lower(bed.tier().name()));
            row.add("energy", store(bed.getEnergy().getAmount(), bed.getEnergy().getCapacity()));
            row.add("nutrient", store(bed.getFluid().getAmount(), bed.getFluid().getCapacity()));
            row.addProperty("has_growth_resources", bed.hasGrowthResources());
            row.addProperty("sealed", GreenhouseIndex.sealedAt(level, pos.above()));
            row.add("crop", crop(level, pos.above()));
            GrowthRules.BlockedReason reason = ResourceCropBlock.plantedGrowthReason(level, pos);
            row.addProperty("blocked_reason", reason == null ? "" : lower(reason.name()));
            beds.add(row);
        }
        out.add("beds", beds);
        out.addProperty("bed_count", beds.size());
        return out;
    }

    /** What is planted at {@code pos}, or a {@code planted:false} stub when the bed is bare. */
    private JsonObject crop(ServerLevel level, BlockPos pos) {
        JsonObject out = new JsonObject();
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof ResourceCropBlock) {
            int age = state.getValue(ResourceCropBlock.AGE);
            out.addProperty("planted", true);
            out.addProperty("kind", "resource");
            out.addProperty("age", age);
            out.addProperty("max_age", ResourceCropBlock.MAX_AGE);
            out.addProperty("growth_percent", percent(age, ResourceCropBlock.MAX_AGE));
            out.addProperty("ready", age >= ResourceCropBlock.MAX_AGE);
            if (level.getBlockEntity(pos) instanceof ResourceCropBlockEntity crop
                    && crop.variant() != null) {
                out.addProperty("material", crop.variant().material().toString());
                out.addProperty("family", lower(crop.variant().family().name()));
                out.addProperty("harvest_count", crop.variant().harvestCount());
            }
            return out;
        }
        if (state.getBlock() instanceof SpeciesCropBlock) {
            int age = state.getValue(SpeciesCropBlock.AGE);
            out.addProperty("planted", true);
            out.addProperty("kind", "species");
            out.addProperty("age", age);
            out.addProperty("max_age", SpeciesCropBlock.MAX_AGE);
            out.addProperty("growth_percent", percent(age, SpeciesCropBlock.MAX_AGE));
            out.addProperty("ready", age >= SpeciesCropBlock.MAX_AGE);
            if (level.getBlockEntity(pos) instanceof SpeciesCropBlockEntity crop) {
                out.addProperty("species", crop.species().toString());
                out.addProperty("harvest_count", crop.harvestCount());
            }
            return out;
        }
        out.addProperty("planted", false);
        return out;
    }

    // --- machines --------------------------------------------------------------

    private JsonObject machines(UUID playerId) {
        JsonObject out = AgricultureLinkAccess.envelope();

        JsonArray fabrication = new JsonArray();
        for (FoundationMachineBlockEntity machine
                : AgricultureLinkAccess.ownedMachines(playerId, FoundationMachineBlockEntity.class)) {
            JsonObject row = machineRow(machine);
            row.addProperty("kind", lower(machine.kind().name()));
            row.addProperty("state", lower(machine.blockedReason().name()));
            row.addProperty("progress", machine.fabricationProgress());
            row.addProperty("progress_max", machine.fabricationMax());
            row.addProperty("progress_percent", percent(machine.fabricationProgress(), machine.fabricationMax()));
            row.add("energy", store(machine.getEnergy().getAmount(), machine.getEnergy().getCapacity()));
            row.add("fluid", store(machine.getFluid().getAmount(), machine.getFluid().getCapacity()));
            fabrication.add(row);
        }
        out.add("fabrication", fabrication);

        JsonArray area = new JsonArray();
        for (AreaMachineBlockEntity machine
                : AgricultureLinkAccess.ownedMachines(playerId, AreaMachineBlockEntity.class)) {
            JsonObject row = machineRow(machine);
            row.addProperty("mode", lower(machine.mode().name()));
            row.add("energy", store(machine.getEnergy().getAmount(), machine.getEnergy().getCapacity()));
            area.add(row);
        }
        out.add("area", area);

        JsonArray terraforming = new JsonArray();
        int total = AgricultureConfig.TERRAFORM_TOTAL_PROGRESS.get();
        for (TerraformingControllerBlockEntity machine
                : AgricultureLinkAccess.ownedMachines(playerId, TerraformingControllerBlockEntity.class)) {
            JsonObject row = machineRow(machine);
            row.addProperty("stage", lower(machine.stage().name()));
            row.addProperty("complete", machine.stage().complete());
            row.addProperty("progress", machine.progress());
            row.addProperty("progress_max", total);
            row.addProperty("progress_percent", percent(machine.progress(), total));
            row.addProperty("radius", AgricultureConfig.TERRAFORM_RADIUS.get());
            row.add("energy", store(machine.getEnergy().getAmount(), machine.getEnergy().getCapacity()));
            row.add("nutrient", store(machine.getFluid().getAmount(), machine.getFluid().getCapacity()));
            terraforming.add(row);
        }
        out.add("terraforming", terraforming);

        out.addProperty("machine_count", fabrication.size() + area.size() + terraforming.size());
        return out;
    }

    /** Identity, place and display name — the fields every machine row starts with. */
    private JsonObject machineRow(net.minecraft.world.level.block.entity.BlockEntity machine) {
        JsonObject row = new JsonObject();
        row.addProperty("id", AgricultureLinkAccess.machineKey(machine));
        row.addProperty("dimension", AgricultureLinkAccess.dimensionOf(machine));
        row.add("position", AgricultureLinkAccess.position(machine.getBlockPos()));
        Identifier block = net.minecraft.core.registries.BuiltInRegistries.BLOCK
                .getKey(machine.getBlockState().getBlock());
        row.addProperty("block", block == null ? "" : block.toString());
        row.addProperty("name", block == null ? "" : AgricultureLinkAccess.readablePath(block.getPath()));
        return row;
    }

    // --- greenhouse ------------------------------------------------------------

    /**
     * The greenhouses enclosing this player's own grow beds. Each bed's crop position is looked up in
     * the interior index; the resulting controllers are de-duplicated, so a greenhouse holding six of
     * the player's beds is one row, not six.
     */
    private JsonObject greenhouses(UUID playerId) {
        JsonObject out = AgricultureLinkAccess.envelope();
        Map<String, JsonObject> byController = new LinkedHashMap<>();
        for (GrowBedBlockEntity bed : AgricultureLinkAccess.ownedMachines(playerId, GrowBedBlockEntity.class)) {
            if (!(bed.getLevel() instanceof ServerLevel level)) {
                continue;
            }
            BlockPos controllerPos = GreenhouseIndex.controllerAt(level, bed.getBlockPos().above());
            if (controllerPos == null) {
                continue;
            }
            String key = level.dimension().identifier() + "@" + controllerPos.asLong();
            JsonObject existing = byController.get(key);
            if (existing != null) {
                existing.addProperty("owned_beds_inside", existing.get("owned_beds_inside").getAsInt() + 1);
                continue;
            }
            JsonObject row = greenhouseRow(level, controllerPos, key);
            if (row != null) {
                byController.put(key, row);
            }
        }
        JsonArray rows = new JsonArray();
        byController.values().forEach(rows::add);
        out.add("greenhouses", rows);
        out.addProperty("greenhouse_count", rows.size());
        return out;
    }

    /** One greenhouse row, or {@code null} when the controller's chunk is not loaded right now. */
    @Nullable
    private JsonObject greenhouseRow(ServerLevel level, BlockPos controllerPos, String key) {
        // Guarded so answering a snapshot can never force-load the controller's chunk.
        if (!level.isLoaded(controllerPos)
                || !(level.getBlockEntity(controllerPos) instanceof GreenhouseControllerBlockEntity controller)) {
            return null;
        }
        JsonObject row = new JsonObject();
        row.addProperty("id", key);
        row.addProperty("dimension", level.dimension().identifier().toString());
        row.add("position", AgricultureLinkAccess.position(controllerPos));
        row.addProperty("state", lower(controller.state().name()));
        row.addProperty("sealed", controller.isActive());
        row.addProperty("volume", controller.volume());
        row.addProperty("volume_cap", AgricultureConfig.GREENHOUSE_VOLUME_CAP.get());
        // An aggregate count of the crops inside — never whose they are.
        row.addProperty("active_crops", controller.activeCrops());
        row.addProperty("oxygen", controller.oxygen());
        row.add("energy", store(controller.getEnergy().getAmount(), controller.getEnergy().getCapacity()));
        row.add("nutrient", store(controller.getFluid().getAmount(), controller.getFluid().getCapacity()));
        BlockPos leak = controller.leak();
        row.addProperty("breached", leak != null);
        if (leak != null) {
            // The hole in a building the requesting player has a machine standing inside; the single
            // most actionable field in the section, and it names nobody.
            row.add("leak", AgricultureLinkAccess.position(leak));
        }
        row.addProperty("owned_beds_inside", 1);
        return row;
    }

    // --- fragments -------------------------------------------------------------

    /**
     * Progress up the Cosmic-ascent ladder — TERRITE → FORGITE → ORBITE → COLONITE → VOIDITE.
     *
     * <p>Two independent things per tier, because they answer different questions. <b>Discovery</b> is
     * how many of the catalog's materials at that tier this player has a {@code material_discovered}
     * milestone for; it is keyed by UUID in Core's store and therefore works whether or not the player
     * is online. <b>The gate</b> is whether that tier's native NeroAgriculture gate
     * ({@code refinement} / {@code synthesis} / {@code transmutation} / {@code ascension}) is open, which
     * Core can only resolve for a live {@code ServerPlayer} — so for an offline player it is reported as
     * {@code null} rather than guessed at. Territe has no gate and is always open.
     */
    private JsonObject fragments(MinecraftServer server, UUID playerId) {
        JsonObject out = AgricultureLinkAccess.envelope();
        Map<Identifier, Set<Identifier>> milestones = MaterialMilestones.exportPlayer(server, playerId);
        Set<Identifier> discovered = milestones.getOrDefault(
                MaterialMilestoneDefinitions.MATERIAL_DISCOVERED, Set.of());
        ServerPlayer player = AgricultureLinkAccess.online(server, playerId);

        Map<FragmentTier, int[]> counts = new LinkedHashMap<>();
        for (FragmentTier tier : FragmentTier.values()) {
            counts.put(tier, new int[2]); // [0] = discovered, [1] = total
        }
        for (ResolvedMaterial material : MaterialCatalog.forServer(server).exposed().values()) {
            int[] tally = counts.get(material.definition().tier());
            if (tally == null) {
                continue;
            }
            tally[1]++;
            if (discovered.contains(material.definition().id())) {
                tally[0]++;
            }
        }

        JsonArray tiers = new JsonArray();
        for (FragmentTier tier : FragmentTier.values()) {
            int[] tally = counts.get(tier);
            JsonObject row = new JsonObject();
            row.addProperty("tier", lower(tier.name()));
            row.addProperty("rank", tier.ordinal() + 1);
            row.addProperty("materials_discovered", tally[0]);
            row.addProperty("materials_total", tally[1]);
            row.addProperty("discovered_percent", percent(tally[0], tally[1]));
            Identifier gate = AgricultureGates.forTier(tier);
            row.addProperty("gate", gate == null ? "" : gate.toString());
            if (gate == null) {
                row.addProperty("gate_open", true); // the first rung is always open
            } else if (player != null) {
                row.addProperty("gate_open", ProgressionGates.isOpen(player, gate));
            } else {
                // Honest: Core resolves gates for a live player only, so offline this is not knowable.
                row.add("gate_open", com.google.gson.JsonNull.INSTANCE);
            }
            tiers.add(row);
        }
        out.add("tiers", tiers);
        out.addProperty("materials_discovered", discovered.size());
        out.addProperty("player_online", player != null);
        return out;
    }

    // --- plumbing --------------------------------------------------------------

    /** A stored/capacity pair, plus the percentage an app would otherwise have to compute. */
    private static JsonObject store(long amount, long capacity) {
        JsonObject out = new JsonObject();
        out.addProperty("stored", amount);
        out.addProperty("capacity", capacity);
        out.addProperty("percent", percent(amount, capacity));
        return out;
    }

    private static int percent(long value, long max) {
        if (max <= 0L) {
            return 0;
        }
        return (int) Math.max(0L, Math.min(100L, 100L * value / max));
    }

    private static String lower(String name) {
        return name.toLowerCase(Locale.ROOT);
    }
}
