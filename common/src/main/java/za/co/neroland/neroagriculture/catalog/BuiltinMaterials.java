package za.co.neroland.neroagriculture.catalog;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.resources.Identifier;

import za.co.neroland.neroagriculture.catalog.CatalogResolver.Candidate;
import za.co.neroland.neroagriculture.catalog.MaterialDefinition.InputSelector;
import za.co.neroland.neroagriculture.catalog.MaterialDefinition.InputSelector.Kind;
import za.co.neroland.neroagriculture.catalog.MaterialDefinition.Yield;
import za.co.neroland.neroagriculture.balance.TierBalance;
import za.co.neroland.neroagriculture.content.EssenceFamily;

/** Explicit conservative defaults; packs can replace every entry by material id. */
public final class BuiltinMaterials {
    private BuiltinMaterials() { }

    public static List<Candidate> candidates() {
        List<Candidate> result = new ArrayList<>();
        ore(result, "coal", "minecraft:coal", EssenceFamily.TERRAN, 0x343434);
        ore(result, "copper", "minecraft:raw_copper", EssenceFamily.INDUSTRIAL, 0xC46B48);
        ore(result, "iron", "minecraft:raw_iron", EssenceFamily.INDUSTRIAL, 0xD8D8D8);
        ore(result, "gold", "minecraft:raw_gold", EssenceFamily.INDUSTRIAL, 0xF4D03F);
        ore(result, "redstone", "minecraft:redstone", EssenceFamily.INDUSTRIAL, 0xAA0000);
        ore(result, "lapis", "minecraft:lapis_lazuli", EssenceFamily.INDUSTRIAL, 0x3154B5);
        ore(result, "quartz", "minecraft:quartz", EssenceFamily.INDUSTRIAL, 0xE8E1D4);
        ore(result, "diamond", "minecraft:diamond", EssenceFamily.ORBITAL, 0x55D6C8);
        ore(result, "emerald", "minecraft:emerald", EssenceFamily.ORBITAL, 0x24C862);
        direct(result, "minecraft:nether_star", "minecraft:nether_star", EssenceFamily.COLONIAL, 0xDDEEFF);
        direct(result, "minecraft:echo_shard", "minecraft:echo_shard", EssenceFamily.DEEPVOID, 0x24545A);
        direct(result, "nerolandcore:nero_alloy", "nerolandcore:nero_alloy_dust", EssenceFamily.INDUSTRIAL, 0x5E7C8C);
        direct(result, "nerolandcore:plasma_glass", "nerolandcore:plasma_glass", EssenceFamily.ORBITAL, 0x82E6FF);
        direct(result, "nerolandcore:void_crystal", "nerolandcore:void_crystal_dust", EssenceFamily.DEEPVOID, 0x5D347A);
        direct(result, "nerolandcore:starsteel", "nerolandcore:starsteel_dust", EssenceFamily.DEEPVOID, 0xC8D1E8);
        return List.copyOf(result);
    }

    private static void ore(List<Candidate> out, String material, String output, EssenceFamily tier, int color) {
        Identifier id = Identifier.parse("c:" + material);
        add(out, id, new InputSelector(Kind.TAG, Identifier.parse("c:ores/" + material)), output, tier, color);
    }

    private static void direct(List<Candidate> out, String material, String output, EssenceFamily tier, int color) {
        Identifier id = Identifier.parse(material);
        add(out, id, new InputSelector(Kind.ITEM, Identifier.parse(output)), output, tier, color);
    }

    private static void add(List<Candidate> out, Identifier id, InputSelector selector, String output,
            EssenceFamily tier, int color) {
        MaterialDefinition definition = new MaterialDefinition(id, selector, Identifier.parse(output), tier,
                MaterialDefinitionParser.defaultGate(tier),
                new Yield(TierBalance.defaultYieldMin(tier), TierBalance.defaultYieldMax(tier), TierBalance.defaultRamp(tier)),
                TierBalance.conversionCount(tier), "material." + id.getNamespace() + "." + id.getPath().replace('/', '.'),
                color, true, null);
        out.add(new Candidate(definition, CatalogSource.BUILTIN, "built-in " + id));
    }
}
