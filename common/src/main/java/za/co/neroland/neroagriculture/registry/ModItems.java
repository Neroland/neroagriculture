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
import za.co.neroland.neroagriculture.content.FragmentTier;
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
    public static final RegistryEntry<Item> RESOURCE_FRAGMENT = variantItem("resource_fragment");
    public static final RegistryEntry<Item> TERRITE_FRAGMENT = item("territe_fragment");
    public static final RegistryEntry<Item> FORGITE_FRAGMENT = item("forgite_fragment");
    public static final RegistryEntry<Item> ORBITE_FRAGMENT = item("orbite_fragment");
    public static final RegistryEntry<Item> COLONITE_FRAGMENT = item("colonite_fragment");
    public static final RegistryEntry<Item> VOIDITE_FRAGMENT = item("voidite_fragment");
    public static final RegistryEntry<Item> PROSPORA_SEED = prosporaSeed();
    public static final RegistryEntry<Item> BLANK_SEED = item("blank_seed");
    public static final RegistryEntry<Item> CHARGED_SEED = chargedSeed();
    public static final RegistryEntry<Item> NUTRIENT_CANISTER = item("nutrient_canister");
    public static final RegistryEntry<BucketItem> NUTRIENT_BUCKET = bucket();
    public static final RegistryEntry<BucketItem> BIOFUEL_BUCKET = biofuelBucket();
    public static final RegistryEntry<Item> FOOD_SEED = speciesSeed("food_seed", za.co.neroland.neroagriculture.food.FoodDefinition.Kind.FOOD);
    public static final RegistryEntry<Item> ALIEN_SEED = speciesSeed("alien_seed", za.co.neroland.neroagriculture.food.FoodDefinition.Kind.ALIEN);
    public static final RegistryEntry<Item> ENGINEERED_FOOD = foodItem("engineered_food");
    public static final RegistryEntry<Item> ALIEN_PRODUCE = foodItem("alien_produce");
    public static final RegistryEntry<Item> FERTILISER = item("fertiliser");
    public static final RegistryEntry<Item> SPEED_FERTILISER = item("speed_fertiliser");
    public static final RegistryEntry<Item> YIELD_FERTILISER = item("yield_fertiliser");
    public static final RegistryEntry<Item> BIOMASS = item("biomass");
    public static final RegistryEntry<Item> CROP_WASTE = item("crop_waste");
    public static final RegistryEntry<Item> BIOFUEL_CANISTER = item("biofuel_canister");
    public static final RegistryEntry<Item> TERRAFORMING_SEED = item("terraforming_seed");
    public static final RegistryEntry<Item> SPEED_MODULE = upgrade("speed_module", UpgradeType.SPEED);
    public static final RegistryEntry<Item> EFFICIENCY_MODULE = upgrade("efficiency_module", UpgradeType.EFFICIENCY);

    static {
        for (RegistryEntry<? extends net.minecraft.world.level.block.Block> block : ModBlocks.ALL) {
            RegistryEntry<BlockItem> entry = ITEMS.register(block.id().getPath(), key ->
                    new BlockItem(block.get(), new Item.Properties().setId(key).useBlockDescriptionPrefix()));
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

    private static RegistryEntry<Item> prosporaSeed() {
        RegistryEntry<Item> entry = ITEMS.register("prospora_seed",
                key -> new za.co.neroland.neroagriculture.content.ProsporaSeedItem(new Item.Properties().setId(key)));
        TAB_ITEMS.add(entry);
        return entry;
    }

    private static RegistryEntry<Item> seedItem(String name) {
        RegistryEntry<Item> entry = ITEMS.register(name, key -> new ResourceSeedItem(new Item.Properties().setId(key)));
        TAB_ITEMS.add(entry);
        return entry;
    }

    private static RegistryEntry<Item> speciesSeed(String name, za.co.neroland.neroagriculture.food.FoodDefinition.Kind kind) {
        RegistryEntry<Item> entry = ITEMS.register(name, key ->
                new za.co.neroland.neroagriculture.content.SpeciesSeedItem(kind, new Item.Properties().setId(key)));
        TAB_ITEMS.add(entry);
        return entry;
    }

    private static RegistryEntry<Item> foodItem(String name) {
        RegistryEntry<Item> entry = ITEMS.register(name, key ->
                new za.co.neroland.neroagriculture.content.SpeciesFoodItem(new Item.Properties().setId(key)
                        .food(new net.minecraft.world.food.FoodProperties.Builder()
                                .nutrition(5).saturationModifier(0.5F).build())));
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

    private static RegistryEntry<BucketItem> biofuelBucket() {
        RegistryEntry<BucketItem> entry = ITEMS.register("biofuel_bucket", key -> new BucketItem(
                za.co.neroland.neroagriculture.fluid.ModFluids.BIOFUEL.get(),
                new Item.Properties().stacksTo(1).setId(key)));
        TAB_ITEMS.add(entry);
        return entry;
    }

    /**
     * Populate NeroAgriculture's own creative tab: every finite item, then one Resource Seed per known
     * resource. The per-resource list is driven by the client's synced material catalog, so it reads
     * like a distinct seed per resource (vanilla + any modded resource discovered through tags) without
     * registering thousands of items. Before the catalog has synced (e.g. on the title screen) it falls
     * back to the curated built-in examples so the tab is never empty.
     */
    public static void populateTab(java.util.function.Consumer<ItemStack> output) {
        for (RegistryEntry<? extends Item> entry : TAB_ITEMS) output.accept(new ItemStack(entry.get()));
        var catalog = za.co.neroland.neroagriculture.catalog.ClientMaterialCatalog.entries();
        if (catalog.isEmpty()) {
            output.accept(example("c:coal", FragmentTier.TERRITE));
            output.accept(example("c:iron", FragmentTier.FORGITE));
            output.accept(example("c:diamond", FragmentTier.ORBITE));
            output.accept(example("minecraft:nether_star", FragmentTier.COLONITE));
            output.accept(example("minecraft:echo_shard", FragmentTier.VOIDITE));
        } else {
            catalog.values().stream()
                    .sorted(java.util.Comparator.comparing(entry -> entry.id().toString()))
                    .forEach(entry -> output.accept(resourceSeed(entry.id(), entry.tier())));
        }
        for (var definition : za.co.neroland.neroagriculture.food.BuiltinFoods.definitions()) {
            boolean alien = definition.kind() == za.co.neroland.neroagriculture.food.FoodDefinition.Kind.ALIEN;
            output.accept(speciesExample(alien ? ALIEN_SEED.get() : FOOD_SEED.get(), definition.id()));
            output.accept(speciesExample(alien ? ALIEN_PRODUCE.get() : ENGINEERED_FOOD.get(), definition.id()));
        }
    }

    private static ItemStack speciesExample(Item item, Identifier species) {
        ItemStack stack = new ItemStack(item);
        stack.set(ModDataComponents.SPECIES_VARIANT.get(),
                za.co.neroland.neroagriculture.content.SpeciesVariant.of(species));
        return stack;
    }

    private static ItemStack example(String id, FragmentTier family) {
        return resourceSeed(Identifier.parse(id), family);
    }

    private static ItemStack resourceSeed(Identifier material, FragmentTier family) {
        ItemStack stack = new ItemStack(RESOURCE_SEED.get());
        stack.set(ModDataComponents.MATERIAL_VARIANT.get(), MaterialVariant.of(material, family));
        za.co.neroland.neroagriculture.content.MaterialTints.apply(stack, material);
        return stack;
    }

    private ModItems() { }
    public static void init() { }
}
