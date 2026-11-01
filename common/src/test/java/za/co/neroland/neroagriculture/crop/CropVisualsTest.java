package za.co.neroland.neroagriculture.crop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import com.google.gson.JsonParser;

import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

class CropVisualsTest {
    @Test void curatedResourcesHaveReadableFamilies() {
        assertEquals(CropVisuals.METAL, CropVisuals.family(Identifier.parse("c:iron")));
        assertEquals(CropVisuals.CRYSTAL, CropVisuals.family(Identifier.parse("nerospace:glacite")));
        assertEquals(CropVisuals.CARBON, CropVisuals.family(Identifier.parse("c:coal")));
    }

    @Test void unknownResourcesUseTheStableGenericFamily() {
        assertEquals(CropVisuals.GENERIC, CropVisuals.family(Identifier.parse("example:unlisted")));
        assertEquals("crystal", CropVisuals.name(CropVisuals.parse("crystal")));
        assertEquals("generic", CropVisuals.name(-1));
    }

    @Test void everyGrowthStageAndFamilyHasAModel() {
        var stream = getClass().getClassLoader().getResourceAsStream(
                "assets/neroagriculture/blockstates/resource_crop.json");
        assertNotNull(stream);
        var variants = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8))
                .getAsJsonObject().getAsJsonObject("variants");
        assertEquals(32, variants.size());
        for (int age = 0; age < 8; age++) {
            for (int family = 0; family < 4; family++) {
                var model = variants.getAsJsonObject("age=" + age + ",visual=" + family).get("model").getAsString();
                assertNotNull(getClass().getClassLoader().getResource("assets/neroagriculture/models/"
                        + model.substring("neroagriculture:".length()) + ".json"));
            }
        }
    }
}
