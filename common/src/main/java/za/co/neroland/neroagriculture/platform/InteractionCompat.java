package za.co.neroland.neroagriculture.platform;

// Minecraft 1.21.1 only (empty on every other line). 1.21.1's Block#useItemOn returns an
// ItemInteractionResult; 1.21.2 folded it into InteractionResult. The blocks keep their logic in
// InteractionResult terms and translate here.

//? if <1.21.2 {
/*import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;

public final class InteractionCompat {

    private InteractionCompat() {
    }

    // PASS from useItemOn means "let the held item act" on every line (1.21.2+ only falls back to the
    // empty-hand interaction for TRY_WITH_EMPTY_HAND), which 1.21.1 spells SKIP_DEFAULT_BLOCK_INTERACTION.
    public static ItemInteractionResult itemResult(InteractionResult result) {
        return switch (result) {
            case SUCCESS, SUCCESS_NO_ITEM_USED -> ItemInteractionResult.SUCCESS;
            case CONSUME -> ItemInteractionResult.CONSUME;
            case CONSUME_PARTIAL -> ItemInteractionResult.CONSUME_PARTIAL;
            case FAIL -> ItemInteractionResult.FAIL;
            default -> ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        };
    }
}
*///?}
