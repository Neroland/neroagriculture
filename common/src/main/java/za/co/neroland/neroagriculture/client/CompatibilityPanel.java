package za.co.neroland.neroagriculture.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import org.jetbrains.annotations.Nullable;

/**
 * Shared "what will actually grow here" side panel for the texture-free crop screens: a titled, scrollable
 * list of every material in the client's synced catalogue, split into a group the machine grows now and a
 * group it cannot yet, each row carrying the material's own colour and its tier tag so the player can read
 * the upgrade ladder straight off the screen. Drawn with {@code fill}/{@code text} like {@link Gauges}, and
 * scrolled with the wheel; the panel owns nothing but its scroll offset.
 */
public final class CompatibilityPanel {
    /** Column width the screens reserve to the right of the 176-wide machine area. */
    public static final int WIDTH = 104;

    private static final int TROUGH = 0xFF2A343C;
    private static final int TROUGH_EDGE = 0xFF1A2229;
    private static final int TITLE = 0xFF1E2C34;
    private static final int GROUP_OK = 0xFF78C860;
    private static final int GROUP_LOCKED = 0xFF9DA9B2;
    private static final int ROW_TEXT = 0xFFE7EEF3;
    private static final int ROW_TEXT_LOCKED = 0xFF7B8892;
    private static final int SCROLL_TRACK = 0xFF1A2229;
    private static final int SCROLL_THUMB = 0xFF6E7C86;

    private static final int ROW_HEIGHT = 10;
    private static final int PAD = 3;
    private static final int TAG_WIDTH = 21;
    private static final int SCROLLBAR_WIDTH = 4;

    private int scroll;
    private int lastX0;
    private int lastY0;
    private int lastX1;
    private int lastY1;
    private int lastLines;
    private int lastVisible = 1;

    /** A drawn line is either a group heading or a material row — scrolling counts both. */
    private record Line(@Nullable Component heading, int headingColor, @Nullable SeedCompatibility.Row row) { }

    /**
     * Draw the panel into {@code (x0,y0)-(x1,y1)} (absolute screen coordinates). {@code acceptedHeadings}
     * is one short line per caption above the growable group (the crop tower uses two: it has no tier gate
     * but its progression gates still bite at growth time). {@code lockedHeading} may be {@code null} for a
     * machine with no tier gate, in which case every row is drawn as accepted anyway.
     */
    public void draw(GuiGraphicsExtractor g, Font font, int x0, int y0, int x1, int y1, Component title,
            List<SeedCompatibility.Row> rows, List<Component> acceptedHeadings,
            @Nullable Component lockedHeading, Component emptyMessage) {
        lastX0 = x0;
        lastY0 = y0;
        lastX1 = x1;
        lastY1 = y1;

        g.text(font, Component.literal(font.plainSubstrByWidth(title.getString(), x1 - x0 - 2)),
                x0 + 1, y0, TITLE, false);
        int listTop = y0 + 11;
        g.fill(x0 - 1, listTop - 1, x1 + 1, y1 + 1, TROUGH_EDGE);
        g.fill(x0, listTop, x1, y1, TROUGH);

        List<Line> lines = lines(rows, acceptedHeadings, lockedHeading);
        int visible = Math.max(1, (y1 - listTop - 2) / ROW_HEIGHT);
        lastLines = lines.size();
        lastVisible = visible;
        scroll = SeedCompatibility.clampScroll(scroll, lines.size(), visible);

        if (lines.isEmpty()) {
            String message = font.plainSubstrByWidth(emptyMessage.getString(), x1 - x0 - PAD * 2);
            g.text(font, Component.literal(message), x0 + PAD, listTop + 3, ROW_TEXT_LOCKED, false);
            return;
        }

        boolean scrollable = lines.size() > visible;
        int textRight = x1 - PAD - (scrollable ? SCROLLBAR_WIDTH + 2 : 0);
        for (int i = 0; i < visible && scroll + i < lines.size(); i++) {
            drawLine(g, font, lines.get(scroll + i), x0 + PAD, listTop + 2 + i * ROW_HEIGHT, textRight);
        }
        if (scrollable) drawScrollbar(g, x1 - PAD - SCROLLBAR_WIDTH, listTop + 2, y1 - 2, lines.size(), visible);
    }

    private static void drawLine(GuiGraphicsExtractor g, Font font, Line line, int left, int top, int right) {
        if (line.heading() != null) {
            // Headings are trimmed like rows so a long translation can never spill out of the trough.
            String heading = font.plainSubstrByWidth(line.heading().getString(), Math.max(0, right - left));
            g.text(font, Component.literal(heading), left, top, line.headingColor(), false);
            return;
        }
        SeedCompatibility.Row row = line.row();
        if (row == null) return;
        boolean accepted = row.accepted();
        // Colour chip in the material's own catalogue colour, dimmed for anything this machine cannot grow.
        int chip = accepted ? 0xFF000000 | row.color() : dim(row.color());
        g.fill(left, top + 1, left + 5, top + 6, chip);
        // Right-aligned tier tag: for accepted rows it is the material's tier, for locked rows it doubles
        // as "the bed you need".
        String tag = SeedCompatibility.tag(row.tier());
        int tagColor = accepted ? GROUP_OK : GROUP_LOCKED;
        g.text(font, Component.literal(tag), right - font.width(tag), top, tagColor, false);
        int nameLeft = left + 8;
        int nameWidth = Math.max(0, right - TAG_WIDTH - nameLeft);
        g.text(font, Component.literal(font.plainSubstrByWidth(row.name(), nameWidth)), nameLeft, top,
                accepted ? ROW_TEXT : ROW_TEXT_LOCKED, false);
    }

    /** Push a catalogue colour most of the way to the trough so a locked row reads as "not yet". */
    private static int dim(int color) {
        int r = ((color >> 16) & 0xFF) * 2 / 5 + 0x2A;
        int gr = ((color >> 8) & 0xFF) * 2 / 5 + 0x34;
        int b = (color & 0xFF) * 2 / 5 + 0x3C;
        return 0xFF000000 | Math.min(0xFF, r) << 16 | Math.min(0xFF, gr) << 8 | Math.min(0xFF, b);
    }

    private void drawScrollbar(GuiGraphicsExtractor g, int x, int top, int bottom, int lines, int visible) {
        g.fill(x, top, x + SCROLLBAR_WIDTH, bottom, SCROLL_TRACK);
        int track = bottom - top;
        int thumb = Math.max(6, track * visible / lines);
        int maxScroll = Math.max(1, lines - visible);
        int offset = (track - thumb) * Math.min(maxScroll, Math.max(0, scroll)) / maxScroll;
        g.fill(x, top + offset, x + SCROLLBAR_WIDTH, top + offset + thumb, SCROLL_THUMB);
    }

    private List<Line> lines(List<SeedCompatibility.Row> rows, List<Component> acceptedHeadings,
            @Nullable Component lockedHeading) {
        if (rows.isEmpty()) return List.of();
        int accepted = SeedCompatibility.acceptedCount(rows);
        List<Line> lines = new ArrayList<>(rows.size() + acceptedHeadings.size() + 1);
        if (accepted > 0) {
            for (Component heading : acceptedHeadings) lines.add(new Line(heading, GROUP_OK, null));
        }
        for (int i = 0; i < accepted; i++) lines.add(new Line(null, 0, rows.get(i)));
        if (accepted < rows.size()) {
            if (lockedHeading != null) lines.add(new Line(lockedHeading, GROUP_LOCKED, null));
            for (int i = accepted; i < rows.size(); i++) lines.add(new Line(null, 0, rows.get(i)));
        }
        return lines;
    }

    /**
     * Wheel handling: scrolls only while the cursor is over the panel, and clamps to the last page so the
     * list can never be scrolled into empty space. Returns whether the event was consumed.
     */
    public boolean scrolled(double mouseX, double mouseY, double scrollY) {
        if (mouseX < lastX0 || mouseX >= lastX1 || mouseY < lastY0 || mouseY >= lastY1) return false;
        if (lastLines <= lastVisible) return false;
        scroll = SeedCompatibility.clampScroll(scroll - (int) Math.signum(scrollY), lastLines, lastVisible);
        return true;
    }
}
