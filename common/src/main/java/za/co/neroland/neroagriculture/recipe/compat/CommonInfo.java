package za.co.neroland.neroagriculture.recipe.compat;

// Minecraft 1.21.x only (empty on 26.x). 26.1 added Recipe.CommonInfo (the shared `show_notification` flag);
// before it FabricationRecipe imports this look-alike instead, with the same JSON field and default.

//? if <26.1 {
/*import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record CommonInfo(boolean showNotification) {

    public static final MapCodec<CommonInfo> MAP_CODEC =
            Codec.BOOL.optionalFieldOf("show_notification", true).xmap(CommonInfo::new, CommonInfo::showNotification);

    public static final StreamCodec<RegistryFriendlyByteBuf, CommonInfo> STREAM_CODEC =
            ByteBufCodecs.BOOL.<RegistryFriendlyByteBuf>cast().map(CommonInfo::new, CommonInfo::showNotification);
}
*///?}
