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

    public static void apply(ItemStack stack, int rgb) {
        stack.set(DataComponents.CUSTOM_MODEL_DATA,
                new CustomModelData(List.of(), List.of(), List.of(), List.of(rgb & 0xFFFFFF)));
    }
}
