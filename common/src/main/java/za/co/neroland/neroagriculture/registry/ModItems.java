package za.co.neroland.neroagriculture.registry;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.BucketItem;

import za.co.neroland.neroagriculture.NeroAgricultureCommon;
import za.co.neroland.neroagriculture.content.EssenceFamily;
import za.co.neroland.neroagriculture.content.MaterialVariant;
import za.co.neroland.nerolandcore.registry.CoreCreativeTab;
import za.co.neroland.nerolandcore.registry.RegistrationProvider;
import za.co.neroland.nerolandcore.registry.RegistrationProvider.RegistryEntry;

/** Finite items plus component-backed examples in the shared Neroland creative tab. */
public final class ModItems {
    public static final RegistrationProvider<Item> ITEMS = RegistrationProvider.get(Registries.ITEM, NeroAgricultureCommon.MOD_ID);
    private static final List<RegistryEntry<? extends Item>> TAB_ITEMS = new ArrayList<>();

    public static final RegistryEntry<Item> RESOURCE_SEED = item("resource_seed");
    public static final RegistryEntry<Item> MATERIAL_ESSENCE = item("material_essence");
    public static final RegistryEntry<Item> TERRAN_ESSENCE = item("terran_essence");
    public static final RegistryEntry<Item> INDUSTRIAL_ESSENCE = item("industrial_essence");
    public static final RegistryEntry<Item> ORBITAL_ESSENCE = item("orbital_essence");
    public static final RegistryEntry<Item> COLONIAL_ESSENCE = item("colonial_essence");
    public static final RegistryEntry<Item> DEEPVOID_ESSENCE = item("deepvoid_essence");
    public static final RegistryEntry<Item> BLANK_SEED = item("blank_seed");
    public static final RegistryEntry<Item> CHARGED_SEED = item("charged_seed");
    public static final RegistryEntry<Item> NUTRIENT_CANISTER = item("nutrient_canister");
    public static final RegistryEntry<BucketItem> NUTRIENT_BUCKET = bucket();
    public static final RegistryEntry<Item> FOOD_SEED = item("food_seed");
    public static final RegistryEntry<Item> ALIEN_SEED = item("alien_seed");
    public static final RegistryEntry<Item> FERTILISER = item("fertiliser");
    public static final RegistryEntry<Item> BIOMASS = item("biomass");
    public static final RegistryEntry<Item> CROP_WASTE = item("crop_waste");
    public static final RegistryEntry<Item> BIOFUEL_CANISTER = item("biofuel_canister");
    public static final RegistryEntry<Item> TERRAFORMING_SEED = item("terraforming_seed");

    static {
        for (RegistryEntry<? extends net.minecraft.world.level.block.Block> block : ModBlocks.ALL) {
            RegistryEntry<BlockItem> entry = ITEMS.register(block.id().getPath(), key ->
                    new BlockItem(block.get(), new Item.Properties().setId(key)));
            TAB_ITEMS.add(entry);
        }
    }

    private static RegistryEntry<Item> item(String name) {
        RegistryEntry<Item> entry = ITEMS.register(name, key -> new Item(new Item.Properties().setId(key)));
        TAB_ITEMS.add(entry);
        return entry;
    }

    private static RegistryEntry<BucketItem> bucket() {
        RegistryEntry<BucketItem> entry = ITEMS.register("nutrient_bucket", key -> new BucketItem(
                za.co.neroland.neroagriculture.fluid.ModFluids.NUTRIENT.get(),
                new Item.Properties().stacksTo(1).setId(key)));
        TAB_ITEMS.add(entry);
        return entry;
    }

    public static void addToCreativeTab() {
        TAB_ITEMS.forEach(CoreCreativeTab::add);
        addExample("minecraft:wheat", EssenceFamily.TERRAN);
        addExample("minecraft:iron_ingot", EssenceFamily.INDUSTRIAL);
        addExample("nerospace:nerosium_ingot", EssenceFamily.ORBITAL);
        addExample("minecraft:emerald", EssenceFamily.COLONIAL);
        addExample("minecraft:echo_shard", EssenceFamily.DEEPVOID);
    }

    private static void addExample(String id, EssenceFamily family) {
        CoreCreativeTab.addStack(() -> {
            ItemStack stack = new ItemStack(RESOURCE_SEED.get());
            stack.set(ModDataComponents.MATERIAL_VARIANT.get(), MaterialVariant.of(Identifier.parse(id), family));
            return stack;
        });
    }

    private ModItems() { }
    public static void init() { }
}
