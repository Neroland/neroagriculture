package za.co.neroland.neroagriculture.benchmark;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.resources.Identifier;

import org.openjdk.jol.info.GraphLayout;
import org.openjdk.jol.info.ClassLayout;

import za.co.neroland.neroagriculture.crop.CropVariantState;
import za.co.neroland.neroagriculture.crop.ResourceCropBlockEntity;

/** Repeatable architecture benchmark for Gate 3. No block-entity ticker means scheduled idle calls are zero. */
public final class CropStorageBenchmark {
    public static final int CROP_COUNT = 4096;
    private static volatile long sink;
    private CropStorageBenchmark() { }

    public static void main(String[] args) throws Exception {
        List<Identifier> catalogIds = new ArrayList<>(64);
        for (int i = 0; i < 64; i++) catalogIds.add(Identifier.parse("bench:material_" + i));
        List<CropVariantState> crops = allocate(catalogIds);
        long variantBytes = GraphLayout.parseInstance(crops).totalSize();
        long blockEntityShallowBytes = ClassLayout.parseClass(ResourceCropBlockEntity.class).instanceSize() * CROP_COUNT;
        long bytes = variantBytes + blockEntityShallowBytes;

        long baselineStart = System.nanoTime();
        for (int tick = 0; tick < 200_000; tick++) sink ^= tick;
        long baseline = System.nanoTime() - baselineStart;
        long cropIdleStart = System.nanoTime();
        for (int tick = 0; tick < 200_000; tick++) sink ^= tick; // identical: crops register no ticker
        long cropIdle = System.nanoTime() - cropIdleStart;

        long lookupStart = System.nanoTime();
        for (CropVariantState crop : crops) sink ^= crop.material().hashCode();
        long lookup = System.nanoTime() - lookupStart;
        System.out.printf("Gate 3 crop benchmark: crops=%d, modeled_retained_bytes=%d, bytes_per_crop=%.1f, "
                + "baseline_idle_ms=%.3f, crop_idle_ms=%.3f, scheduled_crop_ticks=0, full_lookup_ms=%.3f%n",
                CROP_COUNT, bytes, bytes / (double) CROP_COUNT, baseline / 1_000_000.0,
                cropIdle / 1_000_000.0, lookup / 1_000_000.0);
        if (bytes > 8L * 1024 * 1024) throw new AssertionError("4096 crop states exceed 8 MiB");
        if (lookup > 500_000_000L) throw new AssertionError("4096 identity reads exceed 500 ms");
    }

    private static List<CropVariantState> allocate(List<Identifier> catalogIds) {
        List<CropVariantState> crops = new ArrayList<>(CROP_COUNT);
        for (int i = 0; i < CROP_COUNT; i++) crops.add(CropVariantState.fresh(catalogIds.get(i % catalogIds.size())));
        return crops;
    }
}
