package za.co.neroland.neroagriculture.catalog;

import java.util.Objects;

import net.minecraft.resources.Identifier;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.neroagriculture.content.FragmentTier;

/** Complete immutable server-side definition of one component-backed resource material. */
public record MaterialDefinition(Identifier id, InputSelector input, Identifier output, FragmentTier tier,
        @Nullable Identifier gate, Yield yield, int conversion, String displayKey, int color, boolean enabled,
        @Nullable WorldRestriction worldRestriction) {

    public static final int MAX_DISPLAY_KEY_LENGTH = 128;

    public MaterialDefinition {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(input, "input");
        Objects.requireNonNull(output, "output");
        Objects.requireNonNull(tier, "tier");
        Objects.requireNonNull(yield, "yield");
        Objects.requireNonNull(displayKey, "displayKey");
        if (displayKey.isBlank() || displayKey.length() > MAX_DISPLAY_KEY_LENGTH) {
            throw new IllegalArgumentException("display_key must contain 1-" + MAX_DISPLAY_KEY_LENGTH + " characters");
        }
        if (conversion < 1 || conversion > 1_000_000) {
            throw new IllegalArgumentException("conversion must be between 1 and 1000000");
        }
        if ((color & 0xFF000000) != 0) {
            throw new IllegalArgumentException("color must be a 24-bit RGB value");
        }
    }

    public MaterialDefinition withOverrides(MaterialOverride override) {
        FragmentTier resolvedTier = override.tier() == null ? tier : override.tier();
        Identifier resolvedGate = override.gateSpecified() ? override.gate() : gate;
        Yield resolvedYield = override.yield() == null ? yield : override.yield();
        int resolvedConversion = override.conversion() == null ? conversion : override.conversion();
        boolean resolvedEnabled = override.enabled() == null ? enabled : override.enabled();
        int resolvedColor = override.color() == null ? color : override.color();
        return new MaterialDefinition(id, input, output, resolvedTier, resolvedGate, resolvedYield,
                resolvedConversion, displayKey, resolvedColor, resolvedEnabled, worldRestriction);
    }

    public MaterialDefinition disabled() {
        return new MaterialDefinition(id, input, output, tier, gate, yield, conversion, displayKey, color,
                false, worldRestriction);
    }

    public record InputSelector(Kind kind, Identifier id) {
        public InputSelector { Objects.requireNonNull(kind, "kind"); Objects.requireNonNull(id, "id"); }
        public enum Kind { ITEM, TAG }
    }

    public record Yield(int minimum, int maximum, int rampHarvests) {
        public Yield {
            if (minimum < 0 || maximum < minimum || maximum > 4096) {
                throw new IllegalArgumentException("yield requires 0 <= minimum <= maximum <= 4096");
            }
            if (rampHarvests < 0 || rampHarvests > 1_000_000) {
                throw new IllegalArgumentException("yield.ramp_harvests must be between 0 and 1000000");
            }
        }
    }

    public record WorldRestriction(Identifier dimension) {
        public WorldRestriction { Objects.requireNonNull(dimension, "dimension"); }
    }
}
