package za.co.neroland.neroagriculture.compat.viewer;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;

import za.co.neroland.neroagriculture.catalog.ClientMaterialCatalog;
import za.co.neroland.neroagriculture.content.FragmentCharge;
import za.co.neroland.neroagriculture.content.FragmentTier;
import za.co.neroland.neroagriculture.content.MaterialTints;
import za.co.neroland.neroagriculture.content.MaterialVariant;
import za.co.neroland.neroagriculture.content.SpeciesVariant;
import za.co.neroland.neroagriculture.machine.FabricationRules;
import za.co.neroland.neroagriculture.machine.MaterialOperations;
import za.co.neroland.neroagriculture.network.MaterialCatalogSyncPayload;
import za.co.neroland.neroagriculture.recipe.FabricationRecipe;
import za.co.neroland.neroagriculture.registry.ModDataComponents;
import za.co.neroland.neroagriculture.registry.ModItems;

/**
 * One fabrication recipe laid out for a recipe viewer: what goes in, what comes out, and the energy and time
 * it costs. Built from a synced {@link FabricationRecipe} the way {@code FoundationMachineBlockEntity}
 * resolves it, so the pages show what a machine actually consumes and produces — including the extra slots
 * the recipe JSON does not list (a synthesizer's Tier Fragments and Prospora Seed, an infuser's Blank Seed,
 * an extractor's material-tagged Resource Fragment).
 *
 * <p>Material identity (the {@code material} / {@code result_material} fields) is shown on the Resource
 * Fragment and Resource Seed stacks through the same {@code material_variant} component and tint the
 * machines apply, so the stacks read "Iron Fragment", "Steel Seed" and so on. The component needs the
 * material's tier: the recipe's {@code family} when present, else the client's synced material catalog.
 * When neither knows it, the plain item is shown rather than a guessed tier.
 *
 * <p>Viewer-agnostic on purpose: the JEI and EMI plugins both adapt these displays, and this class imports
 * neither viewer.
 *
 * @param page    which viewer page this belongs to
 * @param id      the recipe's id
 * @param inputs  one entry per input slot; each slot cycles through its stacks
 * @param outputs one stack per output slot
 * @param energy  FE per operation, before machine upgrades; 0 for Seed Research, which is instant
 * @param ticks   ticks per operation, before machine upgrades; 0 for Seed Research
 * @param unlockOnly true when the output is unlocked rather than handed over (researching a resource
 *                   records a milestone and gives no item), so viewers must not treat it as a way to make it
 */
public record FabricationDisplay(FabricationPage page, Identifier id, List<List<ItemStack>> inputs,
        List<ItemStack> outputs, int energy, int ticks, boolean unlockOnly) {

    /** Every synced recipe as a display, skipping any that cannot be shown. */
    public static List<FabricationDisplay> all(List<RecipeHolder<FabricationRecipe>> holders) {
        List<FabricationDisplay> out = new ArrayList<>();
        for (RecipeHolder<FabricationRecipe> holder : holders) {
            of(holder).ifPresent(out::add);
        }
        return List.copyOf(out);
    }

    /** The display for one recipe, or empty for an unknown type or a recipe with nothing to show. */
    public static Optional<FabricationDisplay> of(RecipeHolder<FabricationRecipe> holder) {
        FabricationRecipe recipe = holder.value();
        FabricationPage page = FabricationPage.of(recipe.getType());
        if (page == null) {
            return Optional.empty();
        }
        List<List<ItemStack>> inputs = new ArrayList<>();
        List<ItemStack> outputs = new ArrayList<>();
        ItemStack result = recipe.resultTemplate().create();
        Optional<FragmentTier> family = recipe.family();
        boolean unlockOnly = false;

        switch (page) {
            case MATERIAL_EXTRACTION -> {
                inputs.add(stacks(recipe.ingredient(), recipe.inputCount()));
                // The extractor gives Tier Fragments of the material's own tier, plus the material-tagged
                // Resource Fragment.
                Optional<FragmentTier> tier = family.or(() -> recipe.material().flatMap(FabricationDisplay::catalogTier));
                outputs.add(tier.map(t -> new ItemStack(MaterialOperations.neutralFragment(t), result.getCount()))
                        .orElse(result));
                recipe.material().ifPresent(material ->
                        outputs.add(withMaterial(ModItems.RESOURCE_FRAGMENT.get(), 1, material, family)));
            }
            case FRAGMENT_INFUSING -> {
                inputs.add(stacks(recipe.ingredient(), recipe.inputCount()));
                if (result.is(ModItems.CHARGED_SEED.get())) {
                    // Charging consumes one Blank Seed and stamps the family on the Charged Seed.
                    inputs.add(List.of(new ItemStack(ModItems.BLANK_SEED.get())));
                    family.ifPresent(tier -> result.set(ModDataComponents.FRAGMENT_CHARGE.get(), FragmentCharge.of(tier)));
                }
                outputs.add(result);
            }
            case FRAGMENT_FUSION -> {
                Optional<Identifier> material = recipe.material();
                inputs.add(material.isPresent()
                        ? List.of(withMaterial(ModItems.RESOURCE_FRAGMENT.get(), recipe.inputCount(), material.get(),
                                Optional.empty()))
                        : stacks(recipe.ingredient(), recipe.inputCount()));
                recipe.secondary().ifPresent(secondary -> inputs.add(stacks(secondary, recipe.secondaryCount())));
                outputs.add(recipe.resultMaterial()
                        .map(alloy -> withMaterial(ModItems.RESOURCE_SEED.get(), result.getCount(), alloy, Optional.empty()))
                        .orElse(result));
            }
            case SEED_SYNTHESIZING -> {
                // Real resource + N matching Tier Fragments + one Prospora Seed base, as the synthesizer takes.
                inputs.add(stacks(recipe.ingredient(), recipe.inputCount()));
                Optional<FragmentTier> tier = family.or(() -> recipe.material().flatMap(FabricationDisplay::catalogTier));
                tier.ifPresent(t -> inputs.add(List.of(new ItemStack(MaterialOperations.neutralFragment(t),
                        FabricationRules.fragmentsPerSeed(t)))));
                inputs.add(List.of(new ItemStack(ModItems.PROSPORA_SEED.get())));
                outputs.add(recipe.material()
                        .map(material -> withMaterial(ModItems.RESOURCE_SEED.get(), result.getCount(), material, family))
                        .orElse(result));
            }
            case MATERIAL_CONVERSION -> {
                // The material-less fallback recipe converts whatever the catalog says, which the client
                // cannot show faithfully, so only material-specific conversions get a page entry.
                if (recipe.material().isEmpty()) {
                    return Optional.empty();
                }
                inputs.add(List.of(withMaterial(ModItems.RESOURCE_FRAGMENT.get(), recipe.inputCount(),
                        recipe.material().get(), family)));
                outputs.add(result);
            }
            case SEED_RESEARCHING -> {
                inputs.add(stacks(recipe.ingredient(), recipe.inputCount()));
                Optional<Identifier> subject = recipe.material();
                if (result.is(ModItems.FOOD_SEED.get()) || result.is(ModItems.ALIEN_SEED.get())) {
                    // Food and alien research hands over the species seed.
                    subject.ifPresent(species -> {
                        try {
                            result.set(ModDataComponents.SPECIES_VARIANT.get(), SpeciesVariant.of(species));
                        } catch (RuntimeException invalid) {
                            // Unknown species id: show the plain seed.
                        }
                    });
                    outputs.add(result);
                } else {
                    // Resource research only unlocks synthesis of that resource's seed; no item comes out.
                    unlockOnly = true;
                    outputs.add(subject
                            .map(material -> withMaterial(ModItems.RESOURCE_SEED.get(), 1, material, family))
                            .orElse(result));
                }
            }
        }
        inputs.removeIf(List::isEmpty);
        outputs.removeIf(ItemStack::isEmpty);
        if (inputs.isEmpty() || outputs.isEmpty()) {
            return Optional.empty();
        }
        boolean research = page == FabricationPage.SEED_RESEARCHING;
        return Optional.of(new FabricationDisplay(page, holder.id().identifier(), List.copyOf(inputs),
                List.copyOf(outputs), research ? 0 : recipe.energy(), research ? 0 : recipe.ticks(), unlockOnly));
    }

    /** The grey lines under a recipe: energy (when it costs any) and time, or how research works. */
    public List<Component> statLines() {
        List<Component> lines = new ArrayList<>();
        if (page == FabricationPage.SEED_RESEARCHING) {
            lines.add(Component.translatable(unlockOnly ? "gui.neroagriculture.viewer.research_unlock"
                    : "gui.neroagriculture.viewer.research_instant"));
            return lines;
        }
        if (energy > 0) {
            lines.add(Component.translatable("gui.neroagriculture.viewer.energy",
                    String.format(Locale.ROOT, "%,d", energy)));
        }
        lines.add(Component.translatable("gui.neroagriculture.viewer.time",
                String.format(Locale.ROOT, "%.1f", ticks / 20.0)));
        return lines;
    }

    private static List<ItemStack> stacks(Ingredient ingredient, int count) {
        List<ItemStack> out = new ArrayList<>();
        ingredient.items().forEach(holder -> out.add(new ItemStack(holder, count)));
        return out;
    }

    /**
     * A Resource Fragment or Seed carrying a material identity, tinted like the machines tint it. Falls back
     * to the plain item when the tier is unknown or the material id is not a valid variant.
     */
    private static ItemStack withMaterial(Item item, int count, Identifier material, Optional<FragmentTier> family) {
        ItemStack stack = new ItemStack(item, Math.max(1, count));
        Optional<FragmentTier> tier = family.or(() -> catalogTier(material));
        if (tier.isEmpty()) {
            return stack;
        }
        try {
            stack.set(ModDataComponents.MATERIAL_VARIANT.get(), MaterialVariant.of(material, tier.get()));
            MaterialTints.apply(stack, material);
        } catch (RuntimeException invalid) {
            return new ItemStack(item, Math.max(1, count));
        }
        return stack;
    }

    /** A material's tier from the client's synced catalog, if it has arrived. */
    private static Optional<FragmentTier> catalogTier(Identifier material) {
        MaterialCatalogSyncPayload.Entry entry = ClientMaterialCatalog.entries().get(material);
        return entry == null ? Optional.empty() : Optional.of(entry.tier());
    }
}
