package za.co.neroland.neroagriculture.food;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

class ColonyFoodTagTest {
    @Test void commonFoodTagContainsEdibleProduceWithoutReplacingOtherMods() {
        var stream = getClass().getClassLoader().getResourceAsStream("data/c/tags/item/foods.json");
        assertNotNull(stream);
        var tag = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        assertFalse(tag.get("replace").getAsBoolean());
        var values = tag.getAsJsonArray("values").toString();
        assertTrue(values.contains("neroagriculture:engineered_food"));
        assertTrue(values.contains("neroagriculture:alien_produce"));
        assertFalse(values.contains("neroagriculture:food_seed"));
    }
}
