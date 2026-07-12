package za.co.neroland.neroagriculture.cycle;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.neroagriculture.NeroAgricultureCommon;

/**
 * Server-owned, reload-safe cycle catalog. A built-in Overworld season profile ships in code; datapacks add
 * or replace per-dimension profiles under {@code data/neroagriculture/neroagriculture/cycles/<dimension>.json}.
 */
public final class CycleCatalog {
    private static final String DIRECTORY = "neroagriculture/cycles";
    private static final String SUFFIX = ".json";

    @Nullable private static MinecraftServer loadedFor;
    private static Map<Identifier, CycleProfile> current = build(null);

    private CycleCatalog() { }

    public static synchronized Map<Identifier, CycleProfile> forServer(@Nullable MinecraftServer server) {
        if (server == null) return current;
        if (loadedFor != server) {
            current = build(server.getResourceManager());
            loadedFor = server;
        }
        return current;
    }

    @Nullable
    public static CycleProfile profile(@Nullable MinecraftServer server, Identifier dimension) {
        return forServer(server).get(dimension);
    }

    private static Map<Identifier, CycleProfile> build(@Nullable ResourceManager resources) {
        Map<Identifier, CycleProfile> map = new LinkedHashMap<>();
        for (CycleProfile builtin : builtin()) map.put(builtin.dimension(), builtin);
        if (resources != null) {
            for (String error : loadDatapacks(resources, map)) {
                NeroAgricultureCommon.LOGGER.warn("[NeroAgriculture] Cycle: {}", error);
            }
        }
        Map<Identifier, CycleProfile> ordered = new LinkedHashMap<>();
        map.keySet().stream().sorted(Comparator.comparing(Identifier::toString)).forEach(id -> ordered.put(id, map.get(id)));
        return Map.copyOf(ordered);
    }

    private static List<CycleProfile> builtin() {
        // A gentle four-season Overworld cycle over four MC days (24000 ticks each).
        List<CycleProfile.Phase> phases = List.of(
                new CycleProfile.Phase("cycle.neroagriculture.spring", new CycleModifier(1.25F, 1.0F, 1.0F)),
                new CycleProfile.Phase("cycle.neroagriculture.summer", new CycleModifier(1.1F, 1.2F, 1.0F)),
                new CycleProfile.Phase("cycle.neroagriculture.autumn", new CycleModifier(1.0F, 1.1F, 1.0F)),
                new CycleProfile.Phase("cycle.neroagriculture.winter", new CycleModifier(0.7F, 0.9F, 1.0F)));
        return List.of(new CycleProfile(Identifier.parse("minecraft:overworld"), 96_000L, 0L, phases));
    }

    private static List<String> loadDatapacks(ResourceManager resources, Map<Identifier, CycleProfile> into) {
        List<String> errors = new ArrayList<>();
        try {
            Map<Identifier, Resource> files = resources.listResources(DIRECTORY, id -> id.getPath().endsWith(SUFFIX));
            List<Identifier> sorted = files.keySet().stream().sorted(Comparator.comparing(Identifier::toString)).toList();
            for (Identifier file : sorted) {
                Identifier dimension = dimensionId(file);
                if (dimension == null) continue;
                for (Resource layer : resources.getResourceStack(file)) {
                    try (BufferedReader reader = layer.openAsReader()) {
                        JsonElement json = JsonParser.parseReader(reader);
                        if (json == null || !json.isJsonObject()) {
                            errors.add(dimension + ": definition must be a JSON object in " + layer.sourcePackId());
                            continue;
                        }
                        into.put(dimension, parse(dimension, json.getAsJsonObject()));
                    } catch (Exception e) {
                        errors.add(dimension + ": could not read pack " + layer.sourcePackId() + ": " + e.getMessage());
                    }
                }
            }
        } catch (RuntimeException e) {
            errors.add("cycle datapack scan failed: " + e.getMessage());
        }
        return errors;
    }

    private static CycleProfile parse(Identifier dimension, JsonObject json) {
        long period = json.get("period").getAsLong();
        long phaseOffset = json.has("phase_offset") ? json.get("phase_offset").getAsLong() : 0L;
        JsonArray phaseArray = json.getAsJsonArray("phases");
        List<CycleProfile.Phase> phases = new ArrayList<>();
        for (JsonElement element : phaseArray) {
            JsonObject phase = element.getAsJsonObject();
            phases.add(new CycleProfile.Phase(phase.get("display_key").getAsString(), new CycleModifier(
                    optionalFloat(phase, "growth"), optionalFloat(phase, "yield"), optionalFloat(phase, "environment"))));
        }
        return new CycleProfile(dimension, period, phaseOffset, phases);
    }

    private static float optionalFloat(JsonObject json, String key) {
        return json.has(key) && !json.get(key).isJsonNull() ? (float) json.get(key).getAsDouble() : 1.0F;
    }

    @Nullable
    private static Identifier dimensionId(Identifier file) {
        String path = file.getPath();
        if (!path.startsWith(DIRECTORY + "/") || !path.endsWith(SUFFIX)) return null;
        return Identifier.fromNamespaceAndPath(file.getNamespace(),
                path.substring(DIRECTORY.length() + 1, path.length() - SUFFIX.length()));
    }
}
