package za.co.neroland.neroagriculture.food;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.server.level.ServerPlayer;

import za.co.neroland.neroagriculture.api.AgricultureApi;
import za.co.neroland.neroagriculture.machine.MachineProgression;
import za.co.neroland.nerolandcore.progression.MaterialMilestones;

/**
 * Publishes diet/variety metadata for future NeroColonies/Economy/Quests consumers without importing them.
 * Only researched food species are reported, and no player-identifying data leaves this seam.
 */
public final class FoodProviders {
    private FoodProviders() { }

    public static void register() {
        AgricultureApi.DIET.add(FoodProviders::diet);
    }

    private static List<AgricultureApi.DietOutput> diet(ServerPlayer player) {
        List<AgricultureApi.DietOutput> outputs = new ArrayList<>();
        for (FoodDefinition definition : FoodCatalog.forServer(player.level().getServer()).values()) {
            if (definition.kind() != FoodDefinition.Kind.FOOD && definition.kind() != FoodDefinition.Kind.ALIEN) continue;
            var milestone = definition.kind() == FoodDefinition.Kind.ALIEN
                    ? MachineProgression.ALIEN_RESEARCH : MachineProgression.FOOD_RESEARCH;
            if (!MaterialMilestones.isObserved(player, milestone, definition.id())) continue;
            outputs.add(new AgricultureApi.DietOutput(definition.id(), definition.nutrition(), definition.saturation(),
                    theme(definition)));
        }
        return List.copyOf(outputs);
    }

    private static net.minecraft.resources.Identifier theme(FoodDefinition definition) {
        return net.minecraft.resources.Identifier.fromNamespaceAndPath("neroagriculture",
                "theme/" + definition.theme().name().toLowerCase(java.util.Locale.ROOT));
    }
}
