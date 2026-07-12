package za.co.neroland.neroagriculture.food;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.neroagriculture.NeroAgricultureCommon;

/**
 * Server-owned, reload-safe food/alien species catalog. Built-in defaults are shipped in code; datapacks
 * under {@code data/neroagriculture/neroagriculture/foods} add or replace entries by id (last pack wins).
 */
public final class FoodCatalog {
    private static final String DIRECTORY = "neroagriculture/foods";
    private static final String SUFFIX = ".json";

    @Nullable private static MinecraftServer loadedFor;
    private static Map<Identifier, FoodDefinition> current = build(null, new ArrayList<>());

    private FoodCatalog() { }

    public static synchronized Map<Identifier, FoodDefinition> forServer(@Nullable MinecraftServer server) {
        if (server == null) return current;
        if (loadedFor != server) reload(server);
        return current;
    }

    public static synchronized Map<Identifier, FoodDefinition> reload(MinecraftServer server) {
        List<String> errors = new ArrayList<>();
        current = build(server.getResourceManager(), errors);
        loadedFor = server;
        for (String error : errors) NeroAgricultureCommon.LOGGER.warn("[NeroAgriculture] Food: {}", error);
        NeroAgricultureCommon.LOGGER.info("[NeroAgriculture] Food catalog resolved {} species.", current.size());
        return current;
    }

    public static Optional<FoodDefinition> lookup(@Nullable MinecraftServer server, Identifier id) {
        return Optional.ofNullable(forServer(server).get(id));
    }

    private static Map<Identifier, FoodDefinition> build(@Nullable ResourceManager resources, List<String> errors) {
        Map<Identifier, FoodDefinition> merged = new LinkedHashMap<>();
        for (FoodDefinition definition : BuiltinFoods.definitions()) merged.put(definition.id(), definition);
        if (resources != null) {
            for (FoodDefinition definition : loadDatapacks(resources, errors)) merged.put(definition.id(), definition);
        }
        Map<Identifier, FoodDefinition> ordered = new LinkedHashMap<>();
        merged.keySet().stream().sorted(Comparator.comparing(Identifier::toString))
                .forEach(id -> ordered.put(id, merged.get(id)));
        return Map.copyOf(ordered);
    }

    private static List<FoodDefinition> loadDatapacks(ResourceManager resources, List<String> errors) {
        List<FoodDefinition> result = new ArrayList<>();
        try {
            Map<Identifier, Resource> files = resources.listResources(DIRECTORY, id -> id.getPath().endsWith(SUFFIX));
            List<Identifier> sorted = files.keySet().stream().sorted(Comparator.comparing(Identifier::toString)).toList();
            for (Identifier file : sorted) {
                Identifier id = speciesId(file);
                if (id == null) continue;
                FoodDefinition valid = null;
                for (Resource layer : resources.getResourceStack(file)) {
                    try (BufferedReader reader = layer.openAsReader()) {
                        JsonElement json = JsonParser.parseReader(reader);
                        if (json == null || !json.isJsonObject()) {
                            errors.add(id + ": definition must be a JSON object in " + layer.sourcePackId());
                            continue;
                        }
                        FoodDefinitionParser.Result parsed = FoodDefinitionParser.parse(id, json.getAsJsonObject());
                        if (!parsed.valid()) {
                            errors.add(parsed.error() + " (pack " + layer.sourcePackId() + ")");
                            continue;
                        }
                        valid = parsed.definition();
                    } catch (Exception e) {
                        errors.add(id + ": could not read pack " + layer.sourcePackId() + ": " + e.getMessage());
                    }
                }
                if (valid != null) result.add(valid);
            }
        } catch (RuntimeException e) {
            errors.add("food datapack scan failed: " + e.getMessage());
        }
        return result;
    }

    @Nullable
    private static Identifier speciesId(Identifier file) {
        String path = file.getPath();
        if (!path.startsWith(DIRECTORY + "/") || !path.endsWith(SUFFIX)) return null;
        String trimmed = path.substring(DIRECTORY.length() + 1, path.length() - SUFFIX.length());
        // foods/<kind>/<name>.json -> neroagriculture:<kind>/<name> (food/ or alien/)
        return Identifier.fromNamespaceAndPath(file.getNamespace(), trimmed);
    }
}
