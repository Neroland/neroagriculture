package za.co.neroland.neroagriculture.content;

import java.util.List;

import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;

import za.co.neroland.neroagriculture.catalog.MaterialColors;

/**
 * Bakes a resource's ingot colour onto a component-driven stack so seeds, fragments and crops render
 * tinted to that resource. The colour is stored in the vanilla {@code custom_model_data} component's
 * colour list (index 0); the item model's {@code tints} reads it through the built-in
 * {@code minecraft:custom_model_data} tint source — no per-loader colour handler needed, so tinting
 * behaves identically on Fabric, Forge and NeoForge. Colour comes from {@link MaterialColors} (the same
 * ingot-colour resolver the catalog uses for its defaults).
 */
public final class MaterialTints {
    private MaterialTints() { }

    public static void apply(ItemStack stack, Identifier material) {
        apply(stack, MaterialColors.resolve(material.getPath()));
    }

    public static int color(za.co.neroland.neroagriculture.catalog.ResolvedCatalog catalog, Identifier material) {
        return catalog.lookup(material).material().map(value -> value.definition().color())
                .orElseGet(() -> MaterialColors.resolve(material.getPath()));
    }

    public static void apply(ItemStack stack, Identifier material, net.minecraft.server.MinecraftServer server) {
        var catalog = za.co.neroland.neroagriculture.catalog.MaterialCatalog.forServer(server);
        var definition = catalog.lookup(material).material().map(value -> value.definition()).orElse(null);
        apply(stack, definition == null ? color(catalog, material) : definition.color(),
                definition == null ? za.co.neroland.neroagriculture.crop.CropVisuals.family(material)
                        : definition.visualFamily());
    }

    public static void apply(ItemStack stack, int rgb) {
        MaterialVariant variant = stack.get(za.co.neroland.neroagriculture.registry.ModDataComponents.MATERIAL_VARIANT.get());
        int family = variant == null ? za.co.neroland.neroagriculture.crop.CropVisuals.GENERIC
                : za.co.neroland.neroagriculture.crop.CropVisuals.family(variant.material());
        apply(stack, rgb, family);
    }

    public static void apply(ItemStack stack, int rgb, int visualFamily) {
        //? if >=1.21.4 {
        stack.set(DataComponents.CUSTOM_MODEL_DATA,
                new CustomModelData(List.of(), List.of(),
                        List.of(za.co.neroland.neroagriculture.crop.CropVisuals.name(visualFamily)), List.of(rgb & 0xFFFFFF)));
        //?} else {
        /*// 1.21.1's custom model data is a single int; the client's item colour handler reads it as the tint.
        stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(rgb & 0xFFFFFF));
        *///?}
    }
}
