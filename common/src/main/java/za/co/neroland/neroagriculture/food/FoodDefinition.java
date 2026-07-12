package za.co.neroland.neroagriculture.food;

import java.util.Objects;

import com.mojang.serialization.Codec;

import net.minecraft.resources.Identifier;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.neroagriculture.content.EssenceFamily;

/**
 * Immutable server-side definition of one engineered-food or alien crop species. All potency/duration
 * values are clamped to explicit caps so a forged or over-tuned datapack entry can never exceed them.
 */
public record FoodDefinition(Identifier id, Kind kind, boolean natural, EffectCategory effect, int amplifier,
        int durationTicks, int potencyCap, int durationCap, int nutrition, float saturation, EssenceFamily tier,
        PlanetTheme theme, boolean geneticsEligible, @Nullable Identifier gate, String displayKey, int color) {

    public static final int MAX_DISPLAY_KEY_LENGTH = 128;
    public static final int MAX_NUTRITION = 20;
    public static final int MAX_AMPLIFIER = 9;
    public static final int MAX_DURATION_TICKS = 24_000;

    public enum Kind {
        FOOD, ALIEN;
        public static final Codec<Kind> CODEC = Codec.STRING.xmap(
                name -> Kind.valueOf(name.toUpperCase(java.util.Locale.ROOT)),
                kind -> kind.name().toLowerCase(java.util.Locale.ROOT));
    }

    public FoodDefinition {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(effect, "effect");
        Objects.requireNonNull(tier, "tier");
        Objects.requireNonNull(theme, "theme");
        Objects.requireNonNull(displayKey, "displayKey");
        if (displayKey.isBlank() || displayKey.length() > MAX_DISPLAY_KEY_LENGTH) {
            throw new IllegalArgumentException("display_key must contain 1-" + MAX_DISPLAY_KEY_LENGTH + " characters");
        }
        if (nutrition < 0 || nutrition > MAX_NUTRITION) {
            throw new IllegalArgumentException("nutrition must be 0-" + MAX_NUTRITION);
        }
        if (!(saturation >= 0.0F) || saturation > MAX_NUTRITION) {
            throw new IllegalArgumentException("saturation must be 0-" + MAX_NUTRITION);
        }
        if (potencyCap < 0 || potencyCap > MAX_AMPLIFIER) {
            throw new IllegalArgumentException("potency_cap must be 0-" + MAX_AMPLIFIER);
        }
        if (durationCap < 0 || durationCap > MAX_DURATION_TICKS) {
            throw new IllegalArgumentException("duration_cap must be 0-" + MAX_DURATION_TICKS);
        }
        if (amplifier < 0 || durationTicks < 0) {
            throw new IllegalArgumentException("amplifier and duration must be non-negative");
        }
        if ((color & 0xFF000000) != 0) {
            throw new IllegalArgumentException("color must be a 24-bit RGB value");
        }
    }

    /** Amplifier actually granted, never above the cap. */
    public int effectiveAmplifier() {
        return Math.max(0, Math.min(amplifier, potencyCap));
    }

    /** Duration actually granted, never above the cap. */
    public int effectiveDurationTicks() {
        return Math.max(0, Math.min(durationTicks, durationCap));
    }

    /** Natural alien strains must be found in the world; only the Synthesizer path is blocked for them. */
    public boolean synthesizable() {
        return !(kind == Kind.ALIEN && natural);
    }

    public boolean hasEffect() {
        return effect != EffectCategory.NONE && effectiveDurationTicks() > 0;
    }
}
