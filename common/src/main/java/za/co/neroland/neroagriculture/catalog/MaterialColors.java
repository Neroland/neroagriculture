package za.co.neroland.neroagriculture.catalog;

import java.util.Locale;
import java.util.Map;

/**
 * Pure, deterministic colour resolution for auto-discovered materials. Seeds, fragments and crops are
 * tinted to this 24-bit RGB colour. Config per-resource {@code color=} overrides take precedence in the
 * resolver; this supplies the sensible default: a curated palette for common materials, else a stable
 * mid-brightness colour derived from the material id so unknown modded resources still read distinctly.
 */
public final class MaterialColors {
    private MaterialColors() { }

    private static final Map<String, Integer> PALETTE = Map.ofEntries(
            Map.entry("coal", 0x343434),
            Map.entry("charcoal", 0x3A3128),
            Map.entry("copper", 0xC46B48),
            Map.entry("iron", 0xD8D8D8),
            Map.entry("gold", 0xF4D03F),
            Map.entry("redstone", 0xAA0000),
            Map.entry("lapis", 0x3154B5),
            Map.entry("lapis_lazuli", 0x3154B5),
            Map.entry("quartz", 0xE8E1D4),
            Map.entry("diamond", 0x55D6C8),
            Map.entry("emerald", 0x24C862),
            Map.entry("amethyst", 0x9A6BD6),
            Map.entry("netherite", 0x4A3F42),
            Map.entry("netherite_scrap", 0x8A5A44),
            Map.entry("nether_star", 0xDDEEFF),
            Map.entry("echo_shard", 0x24545A),
            Map.entry("tin", 0xC7CCD1),
            Map.entry("lead", 0x6E7385),
            Map.entry("silver", 0xD6E2E8),
            Map.entry("nickel", 0xC9C79A),
            Map.entry("zinc", 0xC2C7CC),
            Map.entry("aluminum", 0xD4D6D8),
            Map.entry("aluminium", 0xD4D6D8),
            Map.entry("cobalt", 0x2C5AA0),
            Map.entry("platinum", 0xBFE0E6),
            Map.entry("iridium", 0xC8D0D8));

    /** Curated colour for {@code materialPath}, or a stable derived colour when unknown. */
    public static int resolve(String materialPath) {
        String path = materialPath.toLowerCase(Locale.ROOT);
        Integer curated = PALETTE.get(path);
        if (curated != null) return curated;
        return derive(path);
    }

    /**
     * Stable, evenly-lit colour from the material id: hue from the hash, fixed mid saturation and
     * value so every generated colour is legible (never near-black or washed out).
     */
    private static int derive(String path) {
        int hash = path.hashCode();
        float hue = ((hash % 360) + 360) % 360 / 360.0f;
        return hsvToRgb(hue, 0.55f, 0.80f);
    }

    private static int hsvToRgb(float h, float s, float v) {
        int i = (int) Math.floor(h * 6);
        float f = h * 6 - i;
        float p = v * (1 - s);
        float q = v * (1 - f * s);
        float t = v * (1 - (1 - f) * s);
        float r;
        float g;
        float b;
        switch (i % 6) {
            case 0 -> { r = v; g = t; b = p; }
            case 1 -> { r = q; g = v; b = p; }
            case 2 -> { r = p; g = v; b = t; }
            case 3 -> { r = p; g = q; b = v; }
            case 4 -> { r = t; g = p; b = v; }
            default -> { r = v; g = p; b = q; }
        }
        int ri = Math.round(r * 255);
        int gi = Math.round(g * 255);
        int bi = Math.round(b * 255);
        return (ri << 16) | (gi << 8) | bi;
    }
}
