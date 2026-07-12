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
import za.co.neroland.neroagriculture.content.MaterialVariantItem;
import za.co.neroland.neroagriculture.content.ResourceSeedItem;
import za.co.neroland.neroagriculture.content.ChargedSeedItem;
import za.co.neroland.neroagriculture.content.AgricultureUpgradeItem;
import za.co.neroland.nerolandcore.upgrade.UpgradeType;
import za.co.neroland.nerolandcore.registry.RegistrationProvider;
import za.co.neroland.nerolandcore.registry.RegistrationProvider.RegistryEntry;

/** Finite items plus component-backed examples shown in NeroAgriculture's own creative tab. */
public final class ModItems {
    public static final RegistrationProvider<Item> ITEMS = RegistrationProvider.get(Registries.ITEM, NeroAgricultureCommon.MOD_ID);
    private static final List<RegistryEntry<? extends Item>> TAB_ITEMS = new ArrayList<>();

    public static final RegistryEntry<Item> RESOURCE_SEED = seedItem("resource_seed");
    public static final RegistryEntry<Item> MATERIAL_ESSENCE = variantItem("material_essence");
    public static final RegistryEntry<Item> TERRAN_ESSENCE = item("terran_essence");
    public static final RegistryEntry<Item> INDUSTRIAL_ESSENCE = item("industrial_essence");
    public static final RegistryEntry<Item> ORBITAL_ESSENCE = item("orbital_essence");
    public static final RegistryEntry<Item> COLONIAL_ESSENCE = item("colonial_essence");
    public static final RegistryEntry<Item> DEEPVOID_ESSENCE = item("deepvoid_essence");
    public static final RegistryEntry<Item> BLANK_SEED = item("blank_seed");
    public static final RegistryEntry<Item> CHARGED_SEED = chargedSeed();
    public static final RegistryEntry<Item> NUTRIENT_CANISTER = item("nutrient_canister");
    public static final RegistryEntry<BucketItem> NUTRIENT_BUCKET = bucket();
    public static final RegistryEntry<Item> FOOD_SEED = item("food_seed");
    public static final RegistryEntry<Item> ALIEN_SEED = item("alien_seed");
    public static final RegistryEntry<Item> FERTILISER = item("fertiliser");
    public static final RegistryEntry<Item> BIOMASS = item("biomass");
    public static final RegistryEntry<Item> CROP_WASTE = item("crop_waste");
    public static final RegistryEntry<Item> BIOFUEL_CANISTER = item("biofuel_canister");
    public static final RegistryEntry<Item> TERRAFORMING_SEED = item("terraforming_seed");
    public static final RegistryEntry<Item> SPEED_MODULE = upgrade("speed_module", UpgradeType.SPEED);
    public static final RegistryEntry<Item> EFFICIENCY_MODULE = upgrade("efficiency_module", UpgradeType.EFFICIENCY);

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

    private static RegistryEntry<Item> variantItem(String name) {
        RegistryEntry<Item> entry = ITEMS.register(name, key -> new MaterialVariantItem(new Item.Properties().setId(key)));
        TAB_ITEMS.add(entry);
        return entry;
    }

    private static RegistryEntry<Item> seedItem(String name) {
        RegistryEntry<Item> entry = ITEMS.register(name, key -> new ResourceSeedItem(new Item.Properties().setId(key)));
        TAB_ITEMS.add(entry);
        return entry;
    }

    private static RegistryEntry<Item> chargedSeed() {
        RegistryEntry<Item> entry = ITEMS.register("charged_seed", key ->
                new ChargedSeedItem(new Item.Properties().setId(key)));
        TAB_ITEMS.add(entry);
        return entry;
    }

    private static RegistryEntry<Item> upgrade(String name, UpgradeType type) {
        RegistryEntry<Item> entry = ITEMS.register(name, key ->
                new AgricultureUpgradeItem(new Item.Properties().setId(key).stacksTo(16), type));
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

    /** Populate NeroAgriculture's own creative tab: every finite item, then component-backed examples. */
    public static void populateTab(java.util.function.Consumer<ItemStack> output) {
        for (RegistryEntry<? extends Item> entry : TAB_ITEMS) output.accept(new ItemStack(entry.get()));
        output.accept(example("c:coal", EssenceFamily.TERRAN));
        output.accept(example("c:iron", EssenceFamily.INDUSTRIAL));
        output.accept(example("c:diamond", EssenceFamily.ORBITAL));
        output.accept(example("minecraft:nether_star", EssenceFamily.COLONIAL));
        output.accept(example("minecraft:echo_shard", EssenceFamily.DEEPVOID));
    }

    private static ItemStack example(String id, EssenceFamily family) {
        ItemStack stack = new ItemStack(RESOURCE_SEED.get());
        stack.set(ModDataComponents.MATERIAL_VARIANT.get(), MaterialVariant.of(Identifier.parse(id), family));
        return stack;
    }

    private ModItems() { }
    public static void init() { }
}
