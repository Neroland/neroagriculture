package za.co.neroland.neroagriculture.environment;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
 * Server-owned, reload-safe local dimension → environment map. Built-in defaults classify vanilla worlds;
 * datapacks under {@code data/neroagriculture/neroagriculture/environments} add or replace by dimension id.
 * Unclassified dimensions default to habitable so third-party worlds are never accidentally made hostile.
 */
public final class DimensionEnvironments {
    private static final String DIRECTORY = "neroagriculture/environments";
    private static final String SUFFIX = ".json";

    @Nullable private static MinecraftServer loadedFor;
    private static Map<Identifier, EnvironmentProfile> current = build(null);

    private DimensionEnvironments() { }

    public static synchronized EnvironmentProfile profileFor(@Nullable MinecraftServer server, Identifier dimension) {
        return forServer(server).getOrDefault(dimension, EnvironmentProfile.HABITABLE);
    }

    public static synchronized Map<Identifier, EnvironmentProfile> forServer(@Nullable MinecraftServer server) {
        if (server == null) return current;
        if (loadedFor != server) {
            current = build(server.getResourceManager());
            loadedFor = server;
        }
        return current;
    }

    private static Map<Identifier, EnvironmentProfile> build(@Nullable ResourceManager resources) {
        Map<Identifier, EnvironmentProfile> map = new LinkedHashMap<>();
        map.put(Identifier.parse("minecraft:overworld"), EnvironmentProfile.HABITABLE);
        map.put(Identifier.parse("minecraft:the_nether"), new EnvironmentProfile(Temperature.HOT, false, true));
        map.put(Identifier.parse("minecraft:the_end"), new EnvironmentProfile(Temperature.TEMPERATE, false, false));
        if (resources != null) {
            for (String error : loadDatapacks(resources, map)) {
                NeroAgricultureCommon.LOGGER.warn("[NeroAgriculture] Environment: {}", error);
            }
        }
        return Map.copyOf(map);
    }

    private static List<String> loadDatapacks(ResourceManager resources, Map<Identifier, EnvironmentProfile> into) {
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
                        into.put(dimension, parse(json.getAsJsonObject()));
                    } catch (Exception e) {
                        errors.add(dimension + ": could not read pack " + layer.sourcePackId() + ": " + e.getMessage());
                    }
                }
            }
        } catch (RuntimeException e) {
            errors.add("environment datapack scan failed: " + e.getMessage());
        }
        return errors;
    }

    private static EnvironmentProfile parse(JsonObject json) {
        Temperature temperature = json.has("temperature") && !json.get("temperature").isJsonNull()
                ? Temperature.valueOf(json.get("temperature").getAsString().toUpperCase(java.util.Locale.ROOT))
                : Temperature.TEMPERATE;
        boolean oxygenated = !json.has("oxygenated") || json.get("oxygenated").getAsBoolean();
        boolean pressurised = !json.has("pressurised") || json.get("pressurised").getAsBoolean();
        return new EnvironmentProfile(temperature, oxygenated, pressurised);
    }

    @Nullable
    private static Identifier dimensionId(Identifier file) {
        String path = file.getPath();
        if (!path.startsWith(DIRECTORY + "/") || !path.endsWith(SUFFIX)) return null;
        String trimmed = path.substring(DIRECTORY.length() + 1, path.length() - SUFFIX.length());
        return Identifier.fromNamespaceAndPath(file.getNamespace(), trimmed);
    }
}
