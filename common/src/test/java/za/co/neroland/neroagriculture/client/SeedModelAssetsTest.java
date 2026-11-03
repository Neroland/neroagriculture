package za.co.neroland.neroagriculture.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import javax.imageio.ImageIO;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

class SeedModelAssetsTest {
    @Test void resourceCropSpritesAreNeutralAndTransparentForMaterialTinting() throws Exception {
        for (int stage = 0; stage < 8; stage++) {
            var stream = getClass().getClassLoader().getResourceAsStream(
                    "assets/neroagriculture/textures/block/resource_crop_stage" + stage + ".png");
            assertNotNull(stream);
            var image = ImageIO.read(stream);
            assertNotNull(image);
            boolean opaque = false;
            boolean transparent = false;
            for (int y = 0; y < image.getHeight(); y++) {
                for (int x = 0; x < image.getWidth(); x++) {
                    int pixel = image.getRGB(x, y);
                    int alpha = pixel >>> 24;
                    if (alpha == 0) { transparent = true; continue; }
                    opaque = true;
                    assertEquals((pixel >>> 16) & 255, (pixel >>> 8) & 255, "stage " + stage);
                    assertEquals((pixel >>> 8) & 255, pixel & 255, "stage " + stage);
                }
            }
            assertTrue(opaque && transparent, "stage " + stage);
        }
    }

    @Test void curatedModelsHaveDistinctGeometryAndTextures() {
        Set<String> geometry = new HashSet<>();
        for (String name : new String[] {"coal", "iron", "copper", "gold", "redstone", "diamond",
                "raw_nerosium", "raw_nerosteel", "xertz_quartz", "cindrite", "glacite", "raw_hematite", "voidglass"}) {
            JsonObject model = resource("models/item/seed/" + name + ".json");
            assertNotNull(model.getAsJsonObject("textures").get("base"));
            geometry.add(model.getAsJsonArray("elements").toString());
            JsonObject fragment = resource("models/item/fragment/" + name + ".json");
            assertEquals(model.getAsJsonArray("elements").size() - 3, fragment.getAsJsonArray("elements").size());
        }
        assertEquals(13, geometry.size());
    }

    @Test void seedModelAlwaysHasAFallback() {
        for (String name : new String[] {"resource_seed", "resource_fragment"}) {
            JsonObject model = resource("items/" + name + ".json").getAsJsonObject("model");
            if ("minecraft:select".equals(model.get("type").getAsString())) {
                assertEquals("neroagriculture:material_variant", model.get("component").getAsString());
                assertEquals(65, model.getAsJsonArray("cases").size());
                for (String material : new String[] {"nerospace:raw_hematite", "nerospace:voidglass"}) {
                    Set<String> families = java.util.stream.StreamSupport.stream(model.getAsJsonArray("cases").spliterator(), false)
                            .map(element -> element.getAsJsonObject().getAsJsonObject("when"))
                            .filter(when -> material.equals(when.get("material").getAsString())
                                    && when.get("version").getAsInt() == 1)
                            .map(when -> when.get("family").getAsString()).collect(java.util.stream.Collectors.toSet());
                    assertEquals(Set.of("territe", "forgite", "orbite", "colonite", "voidite"), families, material);
                }
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
