package za.co.neroland.neroagriculture.tower;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

import za.co.neroland.neroagriculture.config.AgricultureConfig;
import za.co.neroland.nerolandcore.event.ThresholdEvents;

/**
 * Publishes crop-tower farm output on Neroland Core's {@link ThresholdEvents} bus as channel
 * {@code neroagriculture:tower_output}, so world events and quests can react to farm output with no
 * NeroAgriculture class (and no NeroEconomy) involved.
 *
 * <ul>
 *   <li><b>scope</b> — the dimension id (for example {@code minecraft:overworld}); never a player.</li>
 *   <li><b>value</b> — items harvested by every crop tower in that dimension during the last in-game
 *       hour.</li>
 *   <li><b>threshold</b> — {@code crop_tower.output_event_threshold} (0 disables the channel).</li>
 *   <li><b>rising</b> — {@code true} when the hourly total reaches the threshold, {@code false} when it
 *       drops back below it. Each direction fires once per crossing.</li>
 * </ul>
 *
 * <p><b>What "hour" means:</b> one in-game hour, 1,000 game ticks (a 24,000-tick day divided by 24; about
 * 50 real seconds at 20 TPS). It is measured on the server's game time, not the time of day, so sleeping
 * or {@code /time set} never skips or repeats part of the window. The window is rolling, kept in ten
 * 100-tick buckets, so the value is the total of the last 900 to 1,000 ticks.
 *
 * <p>The window lives in memory only: after a restart every dimension starts at zero and below the
 * threshold, so a restart announces nothing. It is server-thread state, cleared when the server stops.
 * Privacy (POPIA/GDPR): counts per dimension only; nothing about players is recorded.
 */
public final class TowerOutputChannel {

    public static final Identifier CHANNEL = Identifier.fromNamespaceAndPath("neroagriculture", "tower_output");

    /** One in-game hour in game ticks. */
    public static final long TICKS_PER_HOUR = 1_000L;
    private static final int BUCKETS = 10;
    private static final long BUCKET_TICKS = TICKS_PER_HOUR / BUCKETS;

    private static final Map<Identifier, Window> WINDOWS = new HashMap<>();

    private TowerOutputChannel() { }

    /** Count {@code amount} items a crop tower in {@code level} just harvested. Server thread only. */
    public static void recordHarvest(ServerLevel level, int amount) {
        if (amount <= 0) return;
        Identifier dimension = level.dimension().identifier();
        Window window = WINDOWS.computeIfAbsent(dimension, key -> new Window());
        window.add(level.getGameTime() / BUCKET_TICKS, amount);
        check(dimension, window);
    }

    /**
     * Age every window once per bucket so a dimension whose towers stop (or unload) falls back below the
     * threshold on time. Called from each loader's server tick; cheap on every other tick.
     */
    public static void serverTick(MinecraftServer server) {
        if (WINDOWS.isEmpty()) return;
        long gameTime = server.overworld().getGameTime();
        if (gameTime % BUCKET_TICKS != 0) return;
        long bucket = gameTime / BUCKET_TICKS;
        Iterator<Map.Entry<Identifier, Window>> it = WINDOWS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Identifier, Window> entry = it.next();
            Window window = entry.getValue();
            window.roll(bucket);
            check(entry.getKey(), window);
            if (!window.above && window.total() == 0) it.remove();
        }
    }

    /** Drop all windows (server stopped). */
    public static void clearAll() {
        WINDOWS.clear();
    }

    private static void check(Identifier dimension, Window window) {
        long threshold = AgricultureConfig.TOWER_OUTPUT_THRESHOLD.get();
        long total = window.total();
        if (threshold <= 0) {
            window.above = false; // channel disabled: forget the state, announce nothing
            return;
        }
        if (!window.above && total >= threshold) {
            window.above = true;
            ThresholdEvents.fire(new ThresholdEvents.ThresholdCrossing(CHANNEL, dimension.toString(), total, threshold, true));
        } else if (window.above && total < threshold) {
            window.above = false;
            ThresholdEvents.fire(new ThresholdEvents.ThresholdCrossing(CHANNEL, dimension.toString(), total, threshold, false));
        }
    }

    /** A rolling one-hour window of harvest counts in {@link #BUCKETS} buckets. */
    static final class Window {
        private final long[] counts = new long[BUCKETS];
        /** Absolute bucket number of the newest bucket; {@link Long#MIN_VALUE} before the first use. */
        private long head = Long.MIN_VALUE;
        boolean above;

        void add(long bucket, long amount) {
            roll(bucket);
            counts[(int) Math.floorMod(head, (long) BUCKETS)] += amount;
        }

        /** Advance to {@code bucket}, clearing every bucket that has aged out of the hour. */
        void roll(long bucket) {
            if (head == Long.MIN_VALUE || bucket - head >= BUCKETS || bucket < head) {
                java.util.Arrays.fill(counts, 0L);
                head = bucket;
                return;
            }
            while (head < bucket) {
                head++;
                counts[(int) Math.floorMod(head, (long) BUCKETS)] = 0L;
            }
        }

        long total() {
            long sum = 0;
            for (long c : counts) sum += c;
            return sum;
        }
    }
}
