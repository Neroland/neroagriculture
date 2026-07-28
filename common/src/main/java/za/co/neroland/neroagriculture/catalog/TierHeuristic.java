package za.co.neroland.neroagriculture.catalog;

import java.util.Locale;
import java.util.Map;

import za.co.neroland.neroagriculture.content.FragmentTier;

/**
 * Pure, deterministic tier assignment for auto-discovered materials. Curated built-in entries always
 * win in the resolver (lower {@link CatalogSource} priority), so this only decides the tier of
 * <em>modded</em> resources found through common tags. The rule is: a small vanilla/Core map first,
 * then rarity keywords in the material path, then a per-tag-category default.
 *
 * <p>Everything here is data-free logic so it can be exercised by unit tests without a server.
 */
public final class TierHeuristic {
    private TierHeuristic() { }

    /** Well-known vanilla + Core materials, mirroring the curated built-ins for consistency. */
    private static final Map<String, FragmentTier> KNOWN = Map.ofEntries(
            Map.entry("coal", FragmentTier.TERRITE),
            Map.entry("charcoal", FragmentTier.TERRITE),
            Map.entry("copper", FragmentTier.FORGITE),
            Map.entry("iron", FragmentTier.FORGITE),
            Map.entry("gold", FragmentTier.FORGITE),
            Map.entry("redstone", FragmentTier.FORGITE),
            Map.entry("lapis", FragmentTier.FORGITE),
            Map.entry("lapis_lazuli", FragmentTier.FORGITE),
            Map.entry("quartz", FragmentTier.FORGITE),
            Map.entry("tin", FragmentTier.FORGITE),
            Map.entry("lead", FragmentTier.FORGITE),
            Map.entry("silver", FragmentTier.FORGITE),
            Map.entry("nickel", FragmentTier.FORGITE),
            Map.entry("zinc", FragmentTier.FORGITE),
            Map.entry("aluminum", FragmentTier.FORGITE),
            Map.entry("aluminium", FragmentTier.FORGITE),
            Map.entry("diamond", FragmentTier.ORBITE),
            Map.entry("emerald", FragmentTier.ORBITE),
            Map.entry("amethyst", FragmentTier.ORBITE),
            Map.entry("netherite", FragmentTier.COLONITE),
            Map.entry("netherite_scrap", FragmentTier.COLONITE),
            Map.entry("nether_star", FragmentTier.COLONITE),
            Map.entry("echo_shard", FragmentTier.VOIDITE));

    /** Path fragments that pull a material up toward the end-game tiers. */
    private static final Map<String, FragmentTier> KEYWORDS = Map.ofEntries(
            Map.entry("void", FragmentTier.VOIDITE),
            Map.entry("star", FragmentTier.VOIDITE),
            Map.entry("cosmic", FragmentTier.VOIDITE),
            Map.entry("end", FragmentTier.VOIDITE),
            Map.entry("chaos", FragmentTier.VOIDITE),
            Map.entry("nether", FragmentTier.COLONITE),
            Map.entry("draconi", FragmentTier.COLONITE),
            Map.entry("titanium", FragmentTier.COLONITE),
            Map.entry("tungsten", FragmentTier.COLONITE),
            Map.entry("cobalt", FragmentTier.ORBITE),
            Map.entry("platinum", FragmentTier.ORBITE),
            Map.entry("iridium", FragmentTier.ORBITE));

    /**
     * Resolve a tier for {@code materialPath} discovered under tag category {@code category}
     * (one of {@code ingots}, {@code gems}, {@code ores}, {@code dusts}, {@code raw_materials}).
     * Unknown/uncategorised materials get the configured {@code discovery.default_tier} (Orbite by
     * default — an unrecognised modded resource reads as space-age rather than early-game).
     */
    public static FragmentTier assign(String materialPath, String category) {
        return assign(materialPath, category, configuredDefault());
    }

    /** Pure variant (unit-testable without a server/config): {@code fallback} replaces the config default. */
    static FragmentTier assign(String materialPath, String category, FragmentTier fallback) {
        String path = materialPath.toLowerCase(Locale.ROOT);
        FragmentTier known = KNOWN.get(path);
        if (known != null) return known;
        FragmentTier best = categoryDefault(category, fallback);
        for (Map.Entry<String, FragmentTier> keyword : KEYWORDS.entrySet()) {
            if (path.contains(keyword.getKey()) && keyword.getValue().ordinal() > best.ordinal()) {
                best = keyword.getValue();
            }
        }
        return best;
    }

    /** The validated {@code discovery.default_tier} config value; fails closed to Orbite. */
    static FragmentTier configuredDefault() {
        try {
            return FragmentTier.valueOf(za.co.neroland.neroagriculture.config.AgricultureConfig
                    .DISCOVERY_DEFAULT_TIER.get().trim().toUpperCase(Locale.ROOT));
        } catch (RuntimeException e) {
            return FragmentTier.ORBITE;
        }
    }

    private static FragmentTier categoryDefault(String category, FragmentTier fallback) {
        return switch (category.toLowerCase(Locale.ROOT)) {
            case "gems" -> FragmentTier.ORBITE;
            default -> fallback;
        };
    }
}
