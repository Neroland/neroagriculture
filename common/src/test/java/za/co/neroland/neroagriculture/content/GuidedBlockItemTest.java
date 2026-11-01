package za.co.neroland.neroagriculture.content;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

class GuidedBlockItemTest {
    @Test void starterGuidesHaveTranslationsAndUnknownBlocksHaveNoGuide() {
        var stream = getClass().getClassLoader().getResourceAsStream("assets/neroagriculture/lang/en_us.json");
        assertNotNull(stream);
        var language = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        for (String name : new String[] {"field_generator", "fragment_extractor", "seed_research_bench",
                "seed_synthesizer", "territe_grow_bed", "forgite_grow_bed", "orbite_grow_bed",
                "colonite_grow_bed", "voidite_grow_bed"}) {
            String key = BlockGuidance.guideKey(name);
            assertNotNull(key, name);
            assertTrue(language.has(key), key);
            assertTrue(language.get(key).getAsString().length() <= 65, key);
        }
        assertTrue(language.has("tooltip.neroagriculture.guide.seed_synthesizer.more"));
        assertEquals(BlockGuidance.guideKey("forgite_grow_bed"), BlockGuidance.guideKey("voidite_grow_bed"));
        assertNull(BlockGuidance.guideKey("unknown_block"));
    }
}
