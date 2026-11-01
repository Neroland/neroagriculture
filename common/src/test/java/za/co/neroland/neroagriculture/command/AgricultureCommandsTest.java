package za.co.neroland.neroagriculture.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

class AgricultureCommandsTest {
    @Test void catalogIdsWorkWithOrWithoutQuotes() {
        assertEquals(Identifier.parse("c:iron"), AgricultureCommands.materialId("c:iron"));
        assertEquals(Identifier.parse("c:iron"), AgricultureCommands.materialId("\"c:iron\""));
    }

    @Test void malformedIdsAreRejected() {
        assertThrows(RuntimeException.class, () -> AgricultureCommands.materialId("c:bad id"));
    }
}
