package za.co.neroland.neroagriculture.catalog;

import net.minecraft.resources.Identifier;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.neroagriculture.content.EssenceFamily;

/** Partial server-config overlay. {@code gateSpecified} distinguishes inherit from explicitly none. */
public record MaterialOverride(@Nullable EssenceFamily tier, @Nullable Identifier gate, boolean gateSpecified,
        @Nullable MaterialDefinition.Yield yield, @Nullable Integer conversion, @Nullable Boolean enabled) {
    public static final MaterialOverride EMPTY = new MaterialOverride(null, null, false, null, null, null);
}
