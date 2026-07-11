package za.co.neroland.neroagriculture.menu;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

import za.co.neroland.neroagriculture.registry.ModMenuTypes;

/** Network-safe empty menu shell; machine-specific slots arrive with their gameplay stages. */
public final class FoundationMachineMenu extends AbstractContainerMenu {
    public FoundationMachineMenu(int containerId, Inventory inventory) {
        super(ModMenuTypes.FOUNDATION_MACHINE.get(), containerId);
    }

    @Override public ItemStack quickMoveStack(Player player, int slot) { return ItemStack.EMPTY; }
    @Override public boolean stillValid(Player player) { return true; }
}
