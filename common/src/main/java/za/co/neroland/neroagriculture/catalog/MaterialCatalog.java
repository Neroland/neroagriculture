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
import za.co.neroland.neroagriculture.config.AgricultureConfig;
import za.co.neroland.neroagriculture.content.EssenceFamily;
import za.co.neroland.nerolandcore.meteor.MeteorMaterials;

/** Server-owned, reload-safe material catalog. */
public final class MaterialCatalog {
    private static final String DIRECTORY = "neroagriculture/materials";
    private static final String SUFFIX = ".json";

    @Nullable private static MinecraftServer loadedFor;
    private static ResolvedCatalog current = CatalogResolver.resolve(BuiltinMaterials.candidates(),
            java.util.Set.of(), java.util.Map.of(), 512, List.of());

    private MaterialCatalog() { }

    public static synchronized ResolvedCatalog forServer(MinecraftServer server) {
        if (loadedFor != server) reload(server);
        return current;
    }

    public static synchronized ResolvedCatalog reload(MinecraftServer server) {
        List<String> errors = new ArrayList<>();
        List<Candidate> candidates = new ArrayList<>();
        candidates.addAll(discoverOreTags(errors));
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
        NeroAgricultureCommon.LOGGER.info("[NeroAgriculture] Material catalog resolved {} active / {} total entries.",
                current.exposed().size(), current.all().size());
        return current;
    }

    public static ResolvedCatalog current() { return current; }

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

    private static List<Candidate> discoverOreTags(List<String> errors) {
        List<Candidate> result = new ArrayList<>();
        BuiltInRegistries.ITEM.getTags()
                .filter(named -> named.key().location().getNamespace().equals("c")
                        && named.key().location().getPath().startsWith("ores/"))
                .sorted(Comparator.comparing(named -> named.key().location().toString()))
                .forEach(named -> {
                    TagKey<Item> tag = named.key();
                    List<Identifier> outputs = java.util.stream.StreamSupport.stream(named.spliterator(), false)
                            .map(holder -> BuiltInRegistries.ITEM.getKey(holder.value()))
                            .filter(java.util.Objects::nonNull).sorted(Comparator.comparing(Identifier::toString)).toList();
                    if (outputs.isEmpty()) {
                        errors.add(tag.location() + ": ore tag is empty");
                        return;
                    }
                    String path = tag.location().getPath().substring("ores/".length());
                    Identifier id = Identifier.fromNamespaceAndPath(tag.location().getNamespace(), path);
                    int color = 0x303030 | (id.toString().hashCode() & 0xCFCFCF);
                    MaterialDefinition definition = new MaterialDefinition(id,
                            new InputSelector(Kind.TAG, tag.location()), outputs.getFirst(), EssenceFamily.ORBITAL,
                            MaterialDefinitionParser.defaultGate(EssenceFamily.ORBITAL), new Yield(1, 5, 96), 16,
                            "material." + id.getNamespace() + "." + id.getPath().replace('/', '.'), color, true, null);
                    result.add(new Candidate(definition, CatalogSource.ORE_TAG, "discovered tag " + tag.location()));
                });
        return result;
    }

    @Nullable
    private static Identifier materialId(Identifier file) {
        String path = file.getPath();
        if (!path.startsWith(DIRECTORY + "/") || !path.endsWith(SUFFIX)) return null;
        return Identifier.fromNamespaceAndPath(file.getNamespace(),
                path.substring(DIRECTORY.length() + 1, path.length() - SUFFIX.length()));
    }
}
