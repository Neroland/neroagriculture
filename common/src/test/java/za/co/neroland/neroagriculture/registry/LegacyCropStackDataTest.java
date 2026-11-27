package za.co.neroland.neroagriculture.registry;

//? if <1.20.5 {
/*import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import net.minecraft.SharedConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import za.co.neroland.neroagriculture.content.FragmentTier;
import za.co.neroland.neroagriculture.content.MaterialVariant;

class LegacyCropStackDataTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void materialAndHarvestCountSurviveStackSerialization() {
        var material = MaterialVariant.of(Identifier.fromNamespaceAndPath("minecraft", "iron"), FragmentTier.FORGITE);
        ItemStack seed = new ItemStack(Items.WHEAT_SEEDS);
        ModDataComponents.MATERIAL_VARIANT.set(seed, material);
        ModDataComponents.HARVEST_COUNT.set(seed, 4);
        ItemStack restored = ItemStack.of(seed.save(new CompoundTag()));
        assertEquals(material, ModDataComponents.MATERIAL_VARIANT.get(restored));
        assertEquals(4, ModDataComponents.HARVEST_COUNT.get(restored));
    }

    @Test
    void invalidVariantRemainsUnreadableAndKeepsOtherStackData() {
        ItemStack seed = new ItemStack(Items.WHEAT_SEEDS);
        seed.getOrCreateTag().putString("neroagriculture:material_variant", "malformed");
        ModDataComponents.HARVEST_COUNT.set(seed, 2);
        assertNull(ModDataComponents.MATERIAL_VARIANT.get(seed));
        assertEquals(2, ModDataComponents.HARVEST_COUNT.get(seed));
    }
}
*///?}
