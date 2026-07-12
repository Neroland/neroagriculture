package za.co.neroland.neroagriculture.food;

import com.google.gson.JsonObject;

import net.minecraft.resources.Identifier;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.neroagriculture.content.EssenceFamily;

/** Strict datapack parser for food/alien species with field-specific errors for logs and diagnostics. */
public final class FoodDefinitionParser {
    public record Result(@Nullable FoodDefinition definition, @Nullable String error) {
        public boolean valid() { return definition != null; }
    }

    private FoodDefinitionParser() { }

    public static Result parse(Identifier id, JsonObject json) {
        try {
            FoodDefinition.Kind kind = enumValue(FoodDefinition.Kind.class, requiredString(json, "kind"), "kind");
            boolean natural = json.has("natural") && json.get("natural").getAsBoolean();
            EffectCategory effect = json.has("effect") && !json.get("effect").isJsonNull()
                    ? enumValue(EffectCategory.class, json.get("effect").getAsString(), "effect")
                    : EffectCategory.NONE;
            int amplifier = optionalInt(json, "amplifier", 0);
            int durationTicks = optionalInt(json, "duration_ticks", 0);
            int potencyCap = optionalInt(json, "potency_cap", amplifier);
            int durationCap = optionalInt(json, "duration_cap", durationTicks);
            int nutrition = requiredInt(json, "nutrition");
            float saturation = (float) json.get("saturation").getAsDouble();
            EssenceFamily tier = enumValue(EssenceFamily.class, requiredString(json, "tier"), "tier");
            PlanetTheme theme = json.has("theme") && !json.get("theme").isJsonNull()
                    ? enumValue(PlanetTheme.class, json.get("theme").getAsString(), "theme")
                    : PlanetTheme.EARTH;
            boolean genetics = !json.has("genetics_eligible") || json.get("genetics_eligible").getAsBoolean();
            Identifier gate = json.has("gate") && !json.get("gate").isJsonNull()
                    ? identifier(json.get("gate").getAsString(), "gate") : null;
            String displayKey = requiredString(json, "display_key");
            int color = parseColor(json);
            int oxygenProduction = optionalInt(json, "oxygen_production", 0);
            return new Result(new FoodDefinition(id, kind, natural, effect, amplifier, durationTicks, potencyCap,
                    durationCap, nutrition, saturation, tier, theme, genetics, gate, displayKey, color, oxygenProduction), null);
        } catch (RuntimeException e) {
            return new Result(null, id + ": " + e.getMessage());
        }
    }

    private static <E extends Enum<E>> E enumValue(Class<E> type, String raw, String field) {
        try { return Enum.valueOf(type, raw.toUpperCase(java.util.Locale.ROOT)); }
        catch (IllegalArgumentException e) { throw new IllegalArgumentException("unknown " + field + " '" + raw + "'"); }
    }

    private static Identifier identifier(String raw, String field) {
        try { return Identifier.parse(raw); }
        catch (RuntimeException e) { throw new IllegalArgumentException(field + " is not a valid identifier: '" + raw + "'"); }
    }

    private static int parseColor(JsonObject json) {
        if (!json.has("color") || json.get("color").isJsonNull()) throw new IllegalArgumentException("missing color");
        var element = json.get("color");
        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()) return element.getAsInt();
        String raw = element.getAsString().trim();
        if (raw.startsWith("#")) raw = raw.substring(1);
        try { return Integer.parseInt(raw, 16); }
        catch (NumberFormatException e) { throw new IllegalArgumentException("color must be RGB integer or #RRGGBB"); }
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

    private static int optionalInt(JsonObject json, String field, int fallback) {
        return json.has(field) && !json.get(field).isJsonNull() ? json.get(field).getAsInt() : fallback;
    }
}
