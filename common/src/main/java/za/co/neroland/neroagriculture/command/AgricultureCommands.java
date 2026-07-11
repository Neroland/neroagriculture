package za.co.neroland.neroagriculture.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import za.co.neroland.neroagriculture.catalog.MaterialCatalog;

/** Operator-only catalog diagnostics. Output contains material metadata only, never player data. */
public final class AgricultureCommands {
    private AgricultureCommands() { }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("neroagriculture")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("catalog")
                        .then(Commands.literal("list").executes(context -> list(context.getSource())))
                        .then(Commands.literal("show")
                                .then(Commands.argument("material", StringArgumentType.string())
                                        .executes(context -> show(context.getSource(),
                                                StringArgumentType.getString(context, "material")))))
                        .then(Commands.literal("errors").executes(context -> errors(context.getSource())))));
    }

    private static int list(CommandSourceStack source) {
        var catalog = MaterialCatalog.forServer(source.getServer());
        source.sendSuccess(() -> Component.literal("[NeroAgriculture] " + catalog.exposed().size()
                + " active / " + catalog.all().size() + " total materials"), false);
        catalog.all().forEach((id, resolved) -> source.sendSuccess(() -> Component.literal("  " + id + " ["
                + resolved.definition().tier().name().toLowerCase() + "] " + resolved.source().name().toLowerCase()
                + (catalog.exposed().containsKey(id) ? "" : " (disabled/not exposed)")), false));
        return Command.SINGLE_SUCCESS;
    }

    private static int show(CommandSourceStack source, String raw) {
        final Identifier id;
        try { id = Identifier.parse(raw); }
        catch (RuntimeException e) { source.sendFailure(Component.literal("Invalid material id: " + raw)); return 0; }
        var lookup = MaterialCatalog.forServer(source.getServer()).lookup(id);
        if (lookup.material().isEmpty()) {
            source.sendFailure(Component.literal("Unknown material: " + id));
            return 0;
        }
        var resolved = lookup.material().orElseThrow();
        var d = resolved.definition();
        source.sendSuccess(() -> Component.literal(id + " status=" + lookup.status().name().toLowerCase()
                + " source=" + resolved.source().name().toLowerCase() + ":" + resolved.sourceDetail()), false);
        source.sendSuccess(() -> Component.literal("  input=" + d.input().kind().name().toLowerCase() + ":" + d.input().id()
                + " output=" + d.output() + " tier=" + d.tier().name().toLowerCase()), false);
        source.sendSuccess(() -> Component.literal("  gate=" + (d.gate() == null ? "none" : d.gate())
                + " yield=" + d.yield().minimum() + ".." + d.yield().maximum() + "@" + d.yield().rampHarvests()
                + " conversion=" + d.conversion() + " color=#" + String.format("%06X", d.color())), false);
        source.sendSuccess(() -> Component.literal("  dimension=" + (d.worldRestriction() == null ? "any"
                : d.worldRestriction().dimension()) + " shadowed=" + resolved.shadowedSources()), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int errors(CommandSourceStack source) {
        var catalog = MaterialCatalog.forServer(source.getServer());
        if (catalog.errors().isEmpty()) {
            source.sendSuccess(() -> Component.literal("[NeroAgriculture] No catalog errors or conflicts."), false);
        } else {
            catalog.errors().forEach(error -> source.sendSuccess(() -> Component.literal("  " + error), false));
        }
        return Command.SINGLE_SUCCESS;
    }
}
