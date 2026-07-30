package za.co.neroland.neroagriculture.client;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

import net.minecraft.resources.Identifier;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.neroagriculture.content.FragmentTier;
import za.co.neroland.neroagriculture.network.MaterialCatalogSyncPayload;

/**
 * Pure partitioning and ordering behind the seed-compatibility panels. The rule a grow bed enforces is
 * exactly {@code bedTier.ordinal() >= materialTier.ordinal()}, so the panel needs nothing from the server
 * beyond the material catalogue the client is already sent — and keeping the maths here (no rendering, no
 * client classes) means it can be unit-tested the same way {@link za.co.neroland.neroagriculture.crop.GrowthRules}
 * is.
 */
public final class SeedCompatibility {
    private SeedCompatibility() { }

    /** One catalogue material as a panel row: {@code accepted} is false when the bed is too low a tier. */
    public record Row(Identifier id, FragmentTier tier, String name, int color, boolean accepted) { }

    /**
     * Accepted rows first, then the locked ones, each group ordered by tier and then display name so the
     * list reads as an upgrade ladder. A {@code null} bed tier means "no tier gate at all" (the crop
     * tower), in which case every material is accepted.
     *
     * @param nameOf resolves an entry's {@code displayKey} to the player's language; supplied by the
     *               caller so this class stays free of client-only translation plumbing.
     */
    public static List<Row> rows(Collection<MaterialCatalogSyncPayload.Entry> entries,
            @Nullable FragmentTier bedTier, Function<MaterialCatalogSyncPayload.Entry, String> nameOf) {
        List<Row> rows = new ArrayList<>(entries.size());
        for (MaterialCatalogSyncPayload.Entry entry : entries) {
            rows.add(new Row(entry.id(), entry.tier(), nameOf.apply(entry), entry.color(),
                    bedTier == null || entry.tier().ordinal() <= bedTier.ordinal()));
        }
        Comparator<Row> order = Comparator.comparing((Row row) -> !row.accepted())
                .thenComparingInt(row -> row.tier().ordinal())
                .thenComparing(Row::name, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(row -> row.id().toString());
        rows.sort(order);
        return List.copyOf(rows);
    }

    /** How many rows at the head of {@link #rows} the bed will actually grow. */
    public static int acceptedCount(List<Row> rows) {
        int count = 0;
        for (Row row : rows) {
            if (!row.accepted()) break;
            count++;
        }
        return count;
    }

    /** Scroll offset clamped so the list can never be scrolled past its last page or above its first. */
    public static int clampScroll(int offset, int lineCount, int visibleLines) {
        return Math.max(0, Math.min(offset, Math.max(0, lineCount - Math.max(1, visibleLines))));
    }

    /**
     * Three-letter tier tag ({@code TER}, {@code FOR}, ...) — the panel columns are narrow. A tier whose
     * name is shorter than three characters is returned whole rather than throwing.
     */
    public static String tag(FragmentTier tier) {
        String name = tier.name();
        return name.substring(0, Math.min(3, name.length()));
    }
}
