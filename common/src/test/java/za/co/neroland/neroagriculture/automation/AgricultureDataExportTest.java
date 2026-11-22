package za.co.neroland.neroagriculture.automation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import com.google.gson.JsonObject;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class AgricultureDataExportTest {
    @AfterEach void clear() {
        za.co.neroland.nerolandcore.data.PlayerDataExport.clearForTests();
    }

    @Test void nothingOwnedExportsNothing() {
        assertNull(AgricultureDataExport.build(List.of()));
    }

    @Test void listsOwnMachinesWithoutAnyIdentifier() {
        JsonObject json = AgricultureDataExport.build(List.of(
                new AgricultureDataExport.Machine("neroagriculture:grow_bed", "minecraft:overworld", 1, 64, -3)));
        assertEquals(1, json.getAsJsonArray("owned_machines").size());
        JsonObject row = json.getAsJsonArray("owned_machines").get(0).getAsJsonObject();
        assertEquals("neroagriculture:grow_bed", row.get("block").getAsString());
        assertEquals(-3, row.get("z").getAsInt());
        String text = json.toString();
        assertFalse(text.matches("(?s).*[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-.*"), "no UUID in the export");
    }

    @Test void registersUnderTheModId() {
        AgricultureDataExport.register();
        assertTrue(za.co.neroland.nerolandcore.data.PlayerDataExport.registeredIds().contains("neroagriculture"));
    }
}
