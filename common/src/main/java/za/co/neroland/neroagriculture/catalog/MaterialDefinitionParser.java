package za.co.neroland.neroagriculture.catalog;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.minecraft.resources.Identifier;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.neroagriculture.content.FragmentTier;

/** Strict datapack parser with field-specific errors suitable for logs and admin diagnostics. */
public final class MaterialDefinitionParser {
    public record Result(@Nullable MaterialDefinition definition, @Nullable String error) {
        public boolean valid() { return definition != null; }
    }

    private MaterialDefinitionParser() { }

    public static Result parse(Identifier id, JsonObject json) {
        try {
            JsonObject selectorJson = requiredObject(json, "input");
            boolean hasItem = selectorJson.has("item");
            boolean hasTag = selectorJson.has("tag");
            if (hasItem == hasTag) throw new IllegalArgumentException("input must contain exactly one of item or tag");
            MaterialDefinition.InputSelector selector = new MaterialDefinition.InputSelector(
                    hasItem ? MaterialDefinition.InputSelector.Kind.ITEM : MaterialDefinition.InputSelector.Kind.TAG,
                    identifier(requiredString(selectorJson, hasItem ? "item" : "tag"), "input"));
            Identifier output = identifier(requiredString(json, "output"), "output");
            FragmentTier tier = tier(requiredString(json, "tier"));
            Identifier gate = json.has("gate")
                    ? (json.get("gate").isJsonNull() ? null : identifier(json.get("gate").getAsString(), "gate"))
                    : defaultGate(tier);
            JsonObject yieldJson = requiredObject(json, "yield");
            MaterialDefinition.Yield yield = new MaterialDefinition.Yield(requiredInt(yieldJson, "minimum"),
                    requiredInt(yieldJson, "maximum"), requiredInt(yieldJson, "ramp_harvests"));
            int conversion = requiredInt(json, "conversion");
            String displayKey = requiredString(json, "display_key");
            int color = parseColor(json.get("color"));
            boolean enabled = !json.has("enabled") || json.get("enabled").getAsBoolean();
            MaterialDefinition.WorldRestriction restriction = json.has("dimension") && !json.get("dimension").isJsonNull()
                    ? new MaterialDefinition.WorldRestriction(identifier(json.get("dimension").getAsString(), "dimension"))
                    : null;
            return new Result(new MaterialDefinition(id, selector, output, tier, gate, yield, conversion,
                    displayKey, color, enabled, restriction), null);
        } catch (RuntimeException e) {
            return new Result(null, id + ": " + e.getMessage());
        }
    }

    @Nullable
    public static Identifier defaultGate(FragmentTier tier) {
        // No hard gates by default (playtest decision 2026-07-16): every tier is open out of the box.
        // Pack-makers can still gate materials explicitly via datapack/config ("gate": id), and the
        // native gate ids in AgricultureGates remain registered + opened for packs that want them.
        return null;
    }

    private static FragmentTier tier(String raw) {
        try { return FragmentTier.valueOf(raw.toUpperCase(java.util.Locale.ROOT)); }
        catch (IllegalArgumentException e) { throw new IllegalArgumentException("unknown tier '" + raw + "'"); }
    }

    private static Identifier identifier(String raw, String field) {
        try { return Identifier.parse(raw); }
        catch (RuntimeException e) { throw new IllegalArgumentException(field + " is not a valid identifier: '" + raw + "'"); }
    }

    /** JSON numbers are decimal RGB; strings go through the shared lenient parser (#/0x/hex/decimal). */
    private static int parseColor(@Nullable JsonElement element) {
        if (element == null || element.isJsonNull()) throw new IllegalArgumentException("missing color");
        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()) {
            int value = element.getAsInt();
            if ((value & 0xFF000000) != 0) throw new IllegalArgumentException("color must be a 24-bit RGB value");
            return value;
        }
        return MaterialColors.parseColor(element.getAsString());
    }

    private static JsonObject requiredObject(JsonObject json, String field) {
        if (!json.has(field) || !json.get(field).isJsonObject()) throw new IllegalArgumentException("missing object " + field);
        return json.getAsJsonObject(field);
    }
    private static String requiredString(JsonObject json, String field) {
        if (!json.has(field) || json.get(field).isJsonNull()) throw new IllegalArgumentException("missing " + field);
        String value = json.get(field).getAsString();
        if (value.isBlank()) throw new IllegalArgumentException(field + " must not be blank");
        return value;
    }
    private static int requiredInt(JsonObject json, String field) {
        if (!json.has(field) || json.get(field).isJsonNull()) throw new IllegalArgumentException("missing " + field);
        return json.get(field).getAsInt();
    }
}
