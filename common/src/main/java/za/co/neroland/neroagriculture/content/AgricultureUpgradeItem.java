package za.co.neroland.neroagriculture.content;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.nerolandcore.upgrade.UpgradeContainer;
import za.co.neroland.nerolandcore.upgrade.UpgradeType;

/** Agriculture module card classified through Core's shared upgrade contract. */
public final class AgricultureUpgradeItem extends Item {
    public static final UpgradeContainer.Classifier CLASSIFIER = AgricultureUpgradeItem::typeOf;
    private final UpgradeType type;
    public AgricultureUpgradeItem(Properties properties, UpgradeType type) { super(properties); this.type = type; }
    @Nullable public static UpgradeType typeOf(ItemStack stack) {
        return stack.getItem() instanceof AgricultureUpgradeItem module ? module.type : null;
    }
}
