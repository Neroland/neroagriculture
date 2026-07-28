package za.co.neroland.neroagriculture.catalog;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.neroagriculture.NeroAgricultureCommon;
import za.co.neroland.neroagriculture.catalog.CatalogResolver.Candidate;
import za.co.neroland.neroagriculture.catalog.MaterialDefinition.InputSelector;
import za.co.neroland.neroagriculture.catalog.MaterialDefinition.InputSelector.Kind;
import za.co.neroland.neroagriculture.catalog.MaterialDefinition.Yield;
import za.co.neroland.neroagriculture.balance.TierBalance;
import za.co.neroland.neroagriculture.config.AgricultureConfig;
import za.co.neroland.neroagriculture.content.FragmentTier;
import za.co.neroland.nerolandcore.meteor.MeteorMaterials;

/** Server-owned, reload-safe material catalog. */
public final class MaterialCatalog {
    private static final String DIRECTORY = "neroagriculture/materials";
    private static final String SUFFIX = ".json";

    @Nullable private static MinecraftServer loadedFor;
    // volatile: reloads write this under the class lock, but current() reads it lock-free from other
    // threads (render/menu paths); without the fence those could see a stale or half-published snapshot.
    private static volatile ResolvedCatalog current = CatalogResolver.resolve(BuiltinMaterials.candidates(),
            java.util.Set.of(), java.util.Map.of(), 512, List.of());

    private MaterialCatalog() { }

    public static synchronized ResolvedCatalog forServer(MinecraftServer server) {
        if (loadedFor != server) reload(server);
        return current;
    }

    public static synchronized ResolvedCatalog reload(MinecraftServer server) {
        List<String> errors = new ArrayList<>();
        List<Candidate> candidates = new ArrayList<>();
        candidates.addAll(discoverTaggedMaterials(errors));
        candidates.addAll(BuiltinMaterials.candidates());
        try {
            MeteorMaterials.reload(server);
            candidates.addAll(MeteorMaterialAdapter.adapt(MeteorMaterials.all(server)));
        } catch (RuntimeException | LinkageError e) {
            errors.add("Core meteor metadata unavailable: " + e.getMessage());
        }
        candidates.addAll(loadDatapacks(server.getResourceManager(), errors));

        CatalogConfigParser.Parsed config = CatalogConfigParser.parse(
                AgricultureConfig.MATERIAL_BLACKLIST.get(), AgricultureConfig.MATERIAL_OVERRIDES.get());
        errors.addAll(config.errors());
        current = CatalogResolver.resolve(candidates, config.blacklist(), config.overrides(),
                AgricultureConfig.DISCOVERY_SCAN_CAP.get(), errors);
        loadedFor = server;
        for (String error : current.errors()) {
            NeroAgricultureCommon.LOGGER.warn("[NeroAgriculture] Catalog: {}", error);
        }
        // Per-id shadowing detail lives at debug level; the errors list carries only the aggregate count.
        for (Map.Entry<Identifier, ResolvedMaterial> entry : current.all().entrySet()) {
            if (entry.getValue().shadowedSources().isEmpty()) continue;
            NeroAgricultureCommon.LOGGER.debug("[NeroAgriculture] Catalog: {} selected {}:{}; shadowed {}",
                    entry.getKey(), entry.getValue().source().name().toLowerCase(java.util.Locale.ROOT),
                    entry.getValue().sourceDetail(), String.join(", ", entry.getValue().shadowedSources()));
        }
        // Blacklist entries that matched nothing used to be silent; one aggregate line makes typos visible.
        List<Identifier> unmatched = config.blacklist().stream()
                .filter(id -> !current.all().containsKey(id))
                .sorted(Comparator.comparing(Identifier::toString)).toList();
        if (!unmatched.isEmpty()) {
            NeroAgricultureCommon.LOGGER.info(
                    "[NeroAgriculture] Catalog: {} blacklist entry(ies) matched no material id: {}",
                    unmatched.size(), unmatched);
        }
        NeroAgricultureCommon.LOGGER.info("[NeroAgriculture] Material catalog resolved {} active / {} total entries.",
                current.exposed().size(), current.all().size());
        return current;
    }

    public static ResolvedCatalog current() { return current; }

    /**
     * Forget the stopped server so its whole object graph can be collected; the next server triggers a
     * fresh {@link #reload} via {@link #forServer}. The resolved catalog itself only references global
     * registry objects, so it may safely outlive the server (e.g. for main-menu tooltips).
     */
    public static synchronized void reset() {
        loadedFor = null;
    }

    private static List<Candidate> loadDatapacks(ResourceManager resources, List<String> errors) {
        List<Candidate> result = new ArrayList<>();
        try {
            Map<Identifier, Resource> files = resources.listResources(DIRECTORY, id -> id.getPath().endsWith(SUFFIX));
            List<Identifier> sorted = files.keySet().stream().sorted(Comparator.comparing(Identifier::toString)).toList();
            for (Identifier file : sorted) {
                Identifier id = materialId(file);
                if (id == null) continue;
                MaterialDefinition valid = null;
                String winningPack = "";
                for (Resource layer : resources.getResourceStack(file)) {
                    try (BufferedReader reader = layer.openAsReader()) {
                        JsonElement json = JsonParser.parseReader(reader);
                        if (json == null || !json.isJsonObject()) {
                            errors.add(id + ": definition must be a JSON object in " + layer.sourcePackId());
                            continue;
                        }
                        MaterialDefinitionParser.Result parsed = MaterialDefinitionParser.parse(id, json.getAsJsonObject());
                        if (!parsed.valid()) {
                            errors.add(parsed.error() + " (pack " + layer.sourcePackId() + ")");
                            continue;
                        }
                        String inputError = validateReferences(parsed.definition());
                        if (inputError != null) {
                            errors.add(id + ": " + inputError + " (pack " + layer.sourcePackId() + ")");
                            continue;
                        }
                        valid = parsed.definition();
                        winningPack = layer.sourcePackId();
                    } catch (Exception e) {
                        errors.add(id + ": could not read pack " + layer.sourcePackId() + ": " + e.getMessage());
                    }
                }
                if (valid != null) result.add(new Candidate(valid, CatalogSource.DATAPACK, "pack " + winningPack));
            }
        } catch (RuntimeException e) {
            errors.add("datapack scan failed: " + e.getMessage());
        }
        return result;
    }

    @Nullable
    private static String validateReferences(MaterialDefinition definition) {
        Item output = BuiltInRegistries.ITEM.getValue(definition.output());
        if (output == null || output == Items.AIR) return "output item does not exist: " + definition.output();
        if (definition.input().kind() == Kind.ITEM) {
            Item input = BuiltInRegistries.ITEM.getValue(definition.input().id());
            if (input == null || input == Items.AIR) return "input item does not exist: " + definition.input().id();
        } else {
            TagKey<Item> key = TagKey.create(net.minecraft.core.registries.Registries.ITEM, definition.input().id());
            if (BuiltInRegistries.ITEM.get(key).isEmpty()) return "input tag does not exist: " + definition.input().id();
        }
        return null;
    }

    /** Tag categories scanned for auto-generation, in output-preference order (ingots/gems win over ore/dust). */
    private static final List<String> DISCOVERY_CATEGORIES = List.of("ingots", "gems", "raw_materials", "dusts", "ores");

    /**
     * Auto-generate a catalog candidate for every resource discovered through the common material tags
     * ({@code c:ingots/*}, {@code c:gems/*}, {@code c:raw_materials/*}, {@code c:dusts/*}, {@code c:ores/*}).
     * The tier comes from {@link TierHeuristic}, the colour from {@link MaterialColors}, and the native
     * gate from the tier — all of which config/datapack entries may override. Curated built-ins and
     * datapack definitions outrank these (lower {@link CatalogSource} priority), so this only fills in
     * modded resources that ship no explicit definition. Recipes still require the real resource, so an
     * absent mod simply yields no craftable seed.
     */
    private static List<Candidate> discoverTaggedMaterials(List<String> errors) {
        List<Candidate> result = new ArrayList<>();
        BuiltInRegistries.ITEM.getTags()
                .filter(named -> named.key().location().getNamespace().equals("c")
                        && category(named.key().location().getPath()) != null)
                .sorted(Comparator.comparing(named -> named.key().location().toString()))
                .forEach(named -> {
                    TagKey<Item> tag = named.key();
                    String category = category(tag.location().getPath());
                    String path = tag.location().getPath().substring(category.length() + 1);
                    List<Identifier> outputs = java.util.stream.StreamSupport.stream(named.spliterator(), false)
                            .map(holder -> BuiltInRegistries.ITEM.getKey(holder.value()))
                            .filter(java.util.Objects::nonNull).sorted(Comparator.comparing(Identifier::toString)).toList();
                    if (outputs.isEmpty()) {
                        errors.add(tag.location() + ": material tag is empty");
                        return;
                    }
                    Identifier id = Identifier.fromNamespaceAndPath(tag.location().getNamespace(), path);
                    FragmentTier tier = TierHeuristic.assign(path, category);
                    int color = MaterialColors.resolve(path);
                    MaterialDefinition definition = new MaterialDefinition(id,
                            new InputSelector(Kind.TAG, tag.location()), outputs.getFirst(), tier,
                            MaterialDefinitionParser.defaultGate(tier),
                            new Yield(TierBalance.defaultYieldMin(tier), TierBalance.defaultYieldMax(tier),
                                    TierBalance.defaultRamp(tier)),
                            TierBalance.conversionCount(tier),
                            "material." + id.getNamespace() + "." + id.getPath().replace('/', '.'), color, true, null);
                    // Rank the detail string so, among same-id discoveries, the preferred category wins the
                    // resolver tiebreak (all share CatalogSource.ORE_TAG priority; detail breaks the tie).
                    String rank = Integer.toString(DISCOVERY_CATEGORIES.indexOf(category));
                    result.add(new Candidate(definition, CatalogSource.ORE_TAG,
                            rank + " discovered " + category + " tag " + tag.location()));
                });
        return result;
    }

    @Nullable
    private static String category(String tagPath) {
        for (String category : DISCOVERY_CATEGORIES) {
            if (tagPath.startsWith(category + "/")) return category;
        }
        return null;
    }

    @Nullable
    private static Identifier materialId(Identifier file) {
        String path = file.getPath();
        if (!path.startsWith(DIRECTORY + "/") || !path.endsWith(SUFFIX)) return null;
        return Identifier.fromNamespaceAndPath(file.getNamespace(),
                path.substring(DIRECTORY.length() + 1, path.length() - SUFFIX.length()));
    }
}
