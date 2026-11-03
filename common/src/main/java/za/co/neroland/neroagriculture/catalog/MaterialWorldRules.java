package za.co.neroland.neroagriculture.catalog;

import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

public final class MaterialWorldRules {
    private MaterialWorldRules() { }

    public static boolean allows(@Nullable Identifier required, Identifier actual) {
        return required == null || required.equals(actual);
    }
}
