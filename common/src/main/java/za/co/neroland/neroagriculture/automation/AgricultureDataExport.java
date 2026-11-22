package za.co.neroland.neroagriculture.automation;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.neroagriculture.NeroAgricultureCommon;

/**
 * NeroAgriculture's part of Neroland Core's per-player data export ({@code /neroland data exportme}), the
 * read-side twin of the automation-owner eraser in {@link AutomationOwner}.
 *
 * <p>The only per-player data NeroAgriculture keeps is the owner UUID on automation machines. The export
 * lists the player's own machines that are currently loaded (block, dimension, position), the same set the
 * eraser and the companion link can reach; machines in unloaded chunks cannot be listed without loading
 * someone's base, which the note in the document says. Built on request, never logged, never written to
 * disk. Erasure tombstones are not exported: they hold no data about the player beyond the fact of
 * erasure, which Core reports itself.</p>
 */
public final class AgricultureDataExport {
    /** One owned machine, already reduced to what the export shows. */
    record Machine(String block, String dimension, int x, int y, int z) { }

    static final String NOTE = "Automation machines you own that were loaded when the export was taken. "
            + "Machines in unloaded areas still carry your owner id and are listed once loaded.";

    private AgricultureDataExport() { }

    /** Register with Core. Call once at common init, next to the eraser. */
    public static void register() {
        za.co.neroland.nerolandcore.data.PlayerDataExport.register(NeroAgricultureCommon.MOD_ID,
                AgricultureDataExport::export);
    }

    @Nullable
    static JsonElement export(MinecraftServer server, UUID player) {
        List<Machine> machines = new ArrayList<>();
        for (AutomationOwner.Owned owned : AutomationOwner.ownedBy(player)) {
            if (owned instanceof BlockEntity be && be.getLevel() != null) {
                Level level = be.getLevel();
                BlockPos pos = be.getBlockPos();
                machines.add(new Machine(String.valueOf(BuiltInRegistries.BLOCK.getKey(be.getBlockState().getBlock())),
                        String.valueOf(level.dimension().identifier()), pos.getX(), pos.getY(), pos.getZ()));
            }
        }
        return build(machines);
    }

    /** Pure layout, pinned by a unit test. {@code null} when the player owns nothing loaded. */
    @Nullable
    static JsonObject build(List<Machine> machines) {
        if (machines.isEmpty()) return null;
        JsonObject json = new JsonObject();
        json.addProperty("note", NOTE);
        JsonArray rows = new JsonArray();
        for (Machine m : machines) {
            JsonObject row = new JsonObject();
            row.addProperty("block", m.block());
            row.addProperty("dimension", m.dimension());
            row.addProperty("x", m.x());
            row.addProperty("y", m.y());
            row.addProperty("z", m.z());
            rows.add(row);
        }
        json.add("owned_machines", rows);
        return json;
    }
}
