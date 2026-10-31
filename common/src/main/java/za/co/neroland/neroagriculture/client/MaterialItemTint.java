package za.co.neroland.neroagriculture.client;

// Minecraft 1.21.1 only (empty on every later line). 1.21.4+ item models tint the Resource Fragment and Seed
// from their custom model data colour (assets/neroagriculture/items/*.json); 1.21.1 has no item model
// definitions, so each loader registers this as the item colour handler for those two items instead.

//? if <1.21.4 {
/*import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;

public final class MaterialItemTint {

    // Same fallback grey as the item model definitions' "default".
    private static final int DEFAULT_RGB = 8947848;

    private MaterialItemTint() {
    }

    public static int color(ItemStack stack, int tintIndex) {
        if (tintIndex != 0) {
            return -1;
        }
        CustomModelData data = stack.get(DataComponents.CUSTOM_MODEL_DATA);
        return 0xFF000000 | ((data == null ? DEFAULT_RGB : data.value()) & 0xFFFFFF);
    }
}
*///?}
