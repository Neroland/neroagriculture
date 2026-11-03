package za.co.neroland.neroagriculture.catalog;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

class MaterialWorldRulesTest {
    @Test void unrestrictedAndMatchingWorldsAreAllowedButOtherWorldsAreNot() {
        var overworld = Identifier.parse("minecraft:overworld");
        var cindara = Identifier.parse("nerospace:cindara");
        assertTrue(MaterialWorldRules.allows(null, overworld));
        assertTrue(MaterialWorldRules.allows(cindara, cindara));
        assertFalse(MaterialWorldRules.allows(cindara, overworld));
    }
}
