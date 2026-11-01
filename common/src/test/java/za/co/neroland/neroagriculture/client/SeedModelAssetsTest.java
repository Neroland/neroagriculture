package za.co.neroland.neroagriculture.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

class SeedModelAssetsTest {
    @Test void curatedModelsHaveDistinctGeometryAndTextures() {
        Set<String> geometry = new HashSet<>();
        for (String name : new String[] {"coal", "iron", "copper", "gold", "redstone", "diamond",
                "raw_nerosium", "raw_nerosteel", "xertz_quartz", "cindrite", "glacite"}) {
            JsonObject model = resource("models/item/seed/" + name + ".json");
            assertNotNull(model.getAsJsonObject("textures").get("base"));
            geometry.add(model.getAsJsonArray("elements").toString());
            JsonObject fragment = resource("models/item/fragment/" + name + ".json");
            assertEquals(model.getAsJsonArray("elements").size() - 3, fragment.getAsJsonArray("elements").size());
        }
        assertEquals(11, geometry.size());
    }

    @Test void seedModelAlwaysHasAFallback() {
        for (String name : new String[] {"resource_seed", "resource_fragment"}) {
            JsonObject model = resource("items/" + name + ".json").getAsJsonObject("model");
            if ("minecraft:select".equals(model.get("type").getAsString())) {
                assertEquals("neroagriculture:material_variant", model.get("component").getAsString());
                assertEquals(55, model.getAsJsonArray("cases").size());
                JsonObject family = model.getAsJsonObject("fallback");
                assertEquals("minecraft:custom_model_data", family.get("property").getAsString());
                assertEquals(3, family.getAsJsonArray("cases").size());
                assertNotEquals("minecraft:missing", family.getAsJsonObject("fallback").get("model").getAsString());
            } else {
                assertEquals("minecraft:model", model.get("type").getAsString());
            }
        }
    }

    private JsonObject resource(String path) {
        var stream = getClass().getClassLoader().getResourceAsStream("assets/neroagriculture/" + path);
        if (stream == null) throw new AssertionError("Missing asset: " + path);
        return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
    }
}
