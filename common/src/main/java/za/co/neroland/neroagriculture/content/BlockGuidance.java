package za.co.neroland.neroagriculture.content;

import java.util.Map;

public final class BlockGuidance {
    private static final Map<String, String> GUIDES = Map.of(
            "field_generator", "field_generator", "fragment_extractor", "fragment_extractor",
            "seed_research_bench", "seed_research_bench", "seed_synthesizer", "seed_synthesizer",
            "territe_grow_bed", "territe_grow_bed", "forgite_grow_bed", "powered_grow_bed",
            "orbite_grow_bed", "powered_grow_bed", "colonite_grow_bed", "powered_grow_bed",
            "voidite_grow_bed", "powered_grow_bed");

    private BlockGuidance() { }

    public static String guideKey(String name) {
        String guide = GUIDES.get(name);
        return guide == null ? null : "tooltip.neroagriculture.guide." + guide;
    }
}
