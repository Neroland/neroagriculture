package za.co.neroland.neroagriculture.catalog;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import net.minecraft.resources.Identifier;

import za.co.neroland.neroagriculture.content.FragmentTier;

/** Parses compact server properties without silently accepting malformed entries. */
public final class CatalogConfigParser {
    public record Parsed(Set<Identifier> blacklist, Map<Identifier, MaterialOverride> overrides, List<String> errors) { }
    private CatalogConfigParser() { }

    public static Parsed parse(String blacklist, String overrides) {
        Set<Identifier> blocked = new LinkedHashSet<>();
        Map<Identifier, MaterialOverride> parsedOverrides = new LinkedHashMap<>();
        List<String> errors = new ArrayList<>();
        for (String raw : blacklist.split(",")) {
            if (raw.isBlank()) continue;
            try { blocked.add(Identifier.parse(raw.trim())); }
            catch (RuntimeException e) { errors.add("blacklist contains invalid id '" + raw.trim() + "'"); }
        }
        // Format: material_id|tier=orbite|gate=neroagriculture:synthesis|color=#55D6C8|yield=1:4:32|conversion=8|enabled=true
        for (String entry : overrides.split(";")) {
            if (entry.isBlank()) continue;
            String[] fields = entry.trim().split("\\|");
            try {
                Identifier id = Identifier.parse(fields[0].trim());
                FragmentTier tier = null;
                Identifier gate = null;
                boolean gateSpecified = false;
                MaterialDefinition.Yield yield = null;
                Integer conversion = null;
                Boolean enabled = null;
                Integer color = null;
                for (int i = 1; i < fields.length; i++) {
                    String[] pair = fields[i].split("=", 2);
                    if (pair.length != 2) throw new IllegalArgumentException("expected key=value at '" + fields[i] + "'");
                    String key = pair[0].trim().toLowerCase(Locale.ROOT);
                    String value = pair[1].trim();
                    switch (key) {
                        case "tier" -> tier = FragmentTier.valueOf(value.toUpperCase(Locale.ROOT));
                        case "gate" -> { gateSpecified = true; gate = value.equalsIgnoreCase("none") ? null : Identifier.parse(value); }
                        case "yield" -> {
                            String[] parts = value.split(":");
                            if (parts.length != 3) throw new IllegalArgumentException("yield must be minimum:maximum:ramp_harvests");
                            yield = new MaterialDefinition.Yield(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
                        }
                        case "conversion" -> conversion = Integer.parseInt(value);
                        case "color" -> color = parseColor(value);
                        case "enabled" -> {
                            if (!value.equalsIgnoreCase("true") && !value.equalsIgnoreCase("false")) throw new IllegalArgumentException("enabled must be true or false");
                            enabled = Boolean.parseBoolean(value);
                        }
                        default -> throw new IllegalArgumentException("unknown override field '" + key + "'");
                    }
                }
                parsedOverrides.put(id, new MaterialOverride(tier, gate, gateSpecified, yield, conversion, enabled, color));
            } catch (RuntimeException e) {
                errors.add("override '" + entry.trim() + "': " + e.getMessage());
            }
        }
        return new Parsed(Set.copyOf(blocked), Map.copyOf(parsedOverrides), List.copyOf(errors));
    }

    /** Shared lenient parser ({@link MaterialColors#parseColor}): #RRGGBB, 0xRRGGBB, bare hex or decimal. */
    private static int parseColor(String raw) {
        return MaterialColors.parseColor(raw);
    }
}
