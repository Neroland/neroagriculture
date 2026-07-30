package za.co.neroland.neroagriculture.crop;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.mojang.serialization.JsonOps;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;
import za.co.neroland.neroagriculture.content.FragmentTier;

class CropVariantStateTest {
    @Test
    void relogRoundTripPreservesLiteralIdTierAndHistory() {
        CropVariantState state = new CropVariantState(CropVariantState.CURRENT_FORMAT,
                Identifier.parse("test:deep_crystal"), FragmentTier.VOIDITE, 73);
        var json = CropVariantState.CODEC.encodeStart(JsonOps.INSTANCE, state).getOrThrow();
        assertEquals(state, CropVariantState.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow());
        assertEquals(74, state.harvested().harvestCount());
    }
}
