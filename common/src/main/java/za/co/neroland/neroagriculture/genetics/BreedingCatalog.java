package za.co.neroland.neroagriculture.genetics;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
 * Server-owned, reload-safe crop breeding tree: an unordered species pair yields a child species, optionally
 * gated by a research milestone. Built-in entries ship in code; datapacks add or replace under
 * {@code data/neroagriculture/neroagriculture/breeding}. The child still passes its own tier/gate/research to
 * grow, so breeding is never a progression bypass.
 */
public final class BreedingCatalog {
    private static final String DIRECTORY = "neroagriculture/breeding";
    private static final String SUFFIX = ".json";

    public record Definition(Identifier id, Identifier parentA, Identifier parentB, Identifier child,
            @Nullable Identifier research) {
        public boolean matches(Identifier a, Identifier b) {
            return parentA.equals(a) && parentB.equals(b) || parentA.equals(b) && parentB.equals(a);
        }
    }

    @Nullable private static MinecraftServer loadedFor;
    private static List<Definition> current = build(null);

    private BreedingCatalog() { }

    public static synchronized List<Definition> forServer(@Nullable MinecraftServer server) {
        if (server == null) return current;
        if (loadedFor != server) {
            current = build(server.getResourceManager());
            loadedFor = server;
        }
        return current;
    }

    public static Optional<Definition> match(@Nullable MinecraftServer server, Identifier a, Identifier b) {
        return forServer(server).stream().filter(definition -> definition.matches(a, b)).findFirst();
    }

    private static List<Definition> build(@Nullable ResourceManager resources) {
        List<Definition> out = new ArrayList<>(builtin());
        if (resources != null) {
            for (String error : loadDatapacks(resources, out)) {
                NeroAgricultureCommon.LOGGER.warn("[NeroAgriculture] Breeding: {}", error);
            }
        }
        out.sort(Comparator.comparing(definition -> definition.id().toString()));
        return List.copyOf(out);
    }

    private static List<Definition> builtin() {
        List<Definition> out = new ArrayList<>();
        out.add(new Definition(id("gloomvine"), food("earth_sunfruit"), food("glacira_glowcap"),
                alien("hybrid_gloomvine"), null));
        return out;
    }

    private static List<String> loadDatapacks(ResourceManager resources, List<Definition> into) {
        List<String> errors = new ArrayList<>();
        try {
            Map<Identifier, Resource> files = resources.listResources(DIRECTORY, id -> id.getPath().endsWith(SUFFIX));
            List<Identifier> sorted = files.keySet().stream().sorted(Comparator.comparing(Identifier::toString)).toList();
            for (Identifier file : sorted) {
                Identifier id = breedingId(file);
                if (id == null) continue;
                for (Resource layer : resources.getResourceStack(file)) {
                    try (BufferedReader reader = layer.openAsReader()) {
                        JsonElement json = JsonParser.parseReader(reader);
                        if (json == null || !json.isJsonObject()) {
                            errors.add(id + ": definition must be a JSON object in " + layer.sourcePackId());
                            continue;
                        }
                        into.removeIf(existing -> existing.id().equals(id));
                        into.add(parse(id, json.getAsJsonObject()));
                    } catch (Exception e) {
                        errors.add(id + ": could not read pack " + layer.sourcePackId() + ": " + e.getMessage());
                    }
                }
            }
        } catch (RuntimeException e) {
            errors.add("breeding datapack scan failed: " + e.getMessage());
        }
        return errors;
    }

    private static Definition parse(Identifier id, JsonObject json) {
        Identifier parentA = Identifier.parse(json.get("parent_a").getAsString());
        Identifier parentB = Identifier.parse(json.get("parent_b").getAsString());
        Identifier child = Identifier.parse(json.get("child").getAsString());
        Identifier research = json.has("research") && !json.get("research").isJsonNull()
                ? Identifier.parse(json.get("research").getAsString()) : null;
        return new Definition(id, parentA, parentB, child, research);
    }

    private static Identifier id(String path) { return Identifier.fromNamespaceAndPath("neroagriculture", path); }
    private static Identifier food(String path) { return Identifier.fromNamespaceAndPath("neroagriculture", "food/" + path); }
    private static Identifier alien(String path) { return Identifier.fromNamespaceAndPath("neroagriculture", "alien/" + path); }

    @Nullable
    private static Identifier breedingId(Identifier file) {
        String path = file.getPath();
        if (!path.startsWith(DIRECTORY + "/") || !path.endsWith(SUFFIX)) return null;
        return Identifier.fromNamespaceAndPath(file.getNamespace(),
                path.substring(DIRECTORY.length() + 1, path.length() - SUFFIX.length()));
    }
}
