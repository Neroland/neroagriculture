package za.co.neroland.neroagriculture.quest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

class FieldLedgerTest {
    @Test void introductoryQuestsAreVisibleNonConsumingAndTranslated() {
        JsonObject chapter = resource("data/neroagriculture/neroquests/chapters/field_ledger.json");
        JsonObject translations = resource("assets/neroagriculture/lang/en_us.json");
        Set<String> listed = new HashSet<>();
        chapter.getAsJsonArray("quests").forEach(entry -> listed.add(entry.getAsJsonObject().get("quest").getAsString()));
        for (String name : new String[] {"starter_power", "first_extraction", "prospora_start", "study_sample", "living_resource"}) {
            assertTrue(listed.contains("neroagriculture:field_ledger/" + name));
            JsonObject quest = resource("data/neroagriculture/neroquests/quests/field_ledger/" + name + ".json");
            assertTrue(quest.getAsJsonArray("prerequisites").isEmpty());
            assertTrue(translations.has(quest.get("title").getAsString()));
            assertTrue(translations.has(quest.get("description").getAsString()));
            JsonObject objective = quest.getAsJsonArray("objectives").get(0).getAsJsonObject();
            assertEquals("neroquests:collect_item", objective.get("type").getAsString());
            assertEquals(quest.get("icon").getAsString(), objective.get("item").getAsString());
            assertFalse(objective.has("consume") && objective.get("consume").getAsBoolean());
        }
    }

    @Test void missingQuestDataFailsClearly() {
        assertThrows(IllegalStateException.class, () -> resource("missing/quest.json"));
    }

    private static JsonObject resource(String path) {
        var stream = FieldLedgerTest.class.getClassLoader().getResourceAsStream(path);
        if (stream == null) throw new IllegalStateException("Missing resource " + path);
        assertNotNull(stream);
        return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
    }
}
