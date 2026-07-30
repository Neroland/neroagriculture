package za.co.neroland.neroagriculture.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import org.jetbrains.annotations.Nullable;

/**
 * Shared "how to build it" side panel for the texture-free screens, in the exact visual style of
 * {@link CompatibilityPanel}: a titled, wheel-scrollable trough drawn with {@code fill}/{@code text}. The
 * content is a flat list of {@link Row}s — word-wrapped text lines (normal, muted, or accent-coloured) and
 * simple diagram rows of small colour cells sketching block layers — and the panel owns nothing but its
 * scroll offset. Text is wrapped with {@link Font#plainSubstrByWidth} (the same primitive every other
 * panel here trims with), backtracking to the last space so words never split mid-glyph.
 */
public final class GuidePanel {
    /**
     * Column width the screens reserve for the panel — deliberately wider than
     * {@link CompatibilityPanel#WIDTH}: guide prose wraps far better at this width than the seed
     * panel's compact name+tag rows need.
     */
    public static final int WIDTH = 150;

    private static final int TROUGH = 0xFF2A343C;
    private static final int TROUGH_EDGE = 0xFF1A2229;
    private static final int TITLE = 0xFF1E2C34;
    private static final int TEXT = 0xFFE7EEF3;
    private static final int MUTED = 0xFF9DA9B2;
    private static final int ACCENT = 0xFF78C860;
    private static final int SCROLL_TRACK = 0xFF1A2229;
    private static final int SCROLL_THUMB = 0xFF6E7C86;

    private static final int ROW_HEIGHT = 10;
    private static final int PAD = 3;
    private static final int CELL = 8;
    private static final int CELL_GAP = 1;
    private static final int SCROLLBAR_WIDTH = 4;

    private int scroll;
    private int lastX0;
    private int lastY0;
    private int lastX1;
    private int lastY1;
    private int lastLines;
    private int lastVisible = 1;

    /**
     * A source row: wrappable text in one of the panel colours, a diagram row of colour cells (ARGB; a
     * {@code 0} cell is an empty gap) with an optional trailing label, or a blank spacer line.
     */
    public record Row(@Nullable Component text, int color, int @Nullable [] cells, @Nullable Component cellLabel) {
        public static Row text(Component text) { return new Row(text, TEXT, null, null); }
        public static Row muted(Component text) { return new Row(text, MUTED, null, null); }
        public static Row accent(Component text) { return new Row(text, ACCENT, null, null); }
        public static Row gap() { return new Row(null, 0, null, null); }
        public static Row cells(@Nullable Component label, int... colors) { return new Row(null, 0, colors, label); }
    }

    /** One rendered line after wrapping — scrolling counts these, not the source rows. */
    private record Line(@Nullable String text, int color, int @Nullable [] cells, @Nullable String cellLabel) { }

    /** Draw the panel into {@code (x0,y0)-(x1,y1)} (absolute screen coordinates). */
    public void draw(GuiGraphicsExtractor g, Font font, int x0, int y0, int x1, int y1, Component title,
            List<Row> rows) {
        lastX0 = x0;
        lastY0 = y0;
        lastX1 = x1;
        lastY1 = y1;

        g.text(font, Component.literal(font.plainSubstrByWidth(title.getString(), x1 - x0 - 2)),
                x0 + 1, y0, TITLE, false);
        int listTop = y0 + 11;
        g.fill(x0 - 1, listTop - 1, x1 + 1, y1 + 1, TROUGH_EDGE);
        g.fill(x0, listTop, x1, y1, TROUGH);

        int visible = Math.max(1, (y1 - listTop - 2) / ROW_HEIGHT);
        // Wrap at the full trough width first; only if that overflows a page re-wrap narrower to make
        // room for the scrollbar (which can only add lines, so scrollability never flips back off).
        List<Line> lines = wrap(font, rows, x1 - x0 - PAD * 2);
        boolean scrollable = lines.size() > visible;
        int textRight = x1 - PAD - (scrollable ? SCROLLBAR_WIDTH + 2 : 0);
        if (scrollable) lines = wrap(font, rows, textRight - (x0 + PAD));
        lastLines = lines.size();
        lastVisible = visible;
        scroll = SeedCompatibility.clampScroll(scroll, lines.size(), visible);

        for (int i = 0; i < visible && scroll + i < lines.size(); i++) {
            drawLine(g, font, lines.get(scroll + i), x0 + PAD, listTop + 2 + i * ROW_HEIGHT, textRight);
        }
        if (scrollable) drawScrollbar(g, x1 - PAD - SCROLLBAR_WIDTH, listTop + 2, y1 - 2, lines.size(), visible);
    }

    private static void drawLine(GuiGraphicsExtractor g, Font font, Line line, int left, int top, int right) {
        if (line.cells() != null) {
            int cx = left;
            for (int color : line.cells()) {
                if (color != 0) g.fill(cx, top, cx + CELL, top + CELL, color);
                cx += CELL + CELL_GAP;
            }
            if (line.cellLabel() != null) {
                String label = font.plainSubstrByWidth(line.cellLabel(), Math.max(0, right - cx - 2));
                g.text(font, Component.literal(label), cx + 2, top, MUTED, false);
            }
            return;
        }
        if (line.text() == null || line.text().isEmpty()) return;
        g.text(font, Component.literal(line.text()), left, top, line.color(), false);
    }

    /** Source rows flattened to drawn lines: text wrapped to {@code width}, cells and spacers passed through. */
    private static List<Line> wrap(Font font, List<Row> rows, int width) {
        List<Line> lines = new ArrayList<>();
        for (Row row : rows) {
            if (row.cells() != null) {
                lines.add(new Line(null, 0, row.cells(),
                        row.cellLabel() == null ? null : row.cellLabel().getString()));
                continue;
            }
            if (row.text() == null) {
                lines.add(new Line(null, 0, null, null));
                continue;
            }
            String remaining = row.text().getString().strip();
            if (remaining.isEmpty()) {
                lines.add(new Line(null, 0, null, null));
                continue;
            }
            while (!remaining.isEmpty()) {
                String head = font.plainSubstrByWidth(remaining, Math.max(1, width));
                if (head.length() < remaining.length()) {
                    int space = head.lastIndexOf(' ');
                    if (space > 0) head = head.substring(0, space);
                }
                if (head.isEmpty()) head = remaining.substring(0, 1); // a glyph wider than the panel: never stall
                lines.add(new Line(head, row.color(), null, null));
                remaining = remaining.substring(head.length()).stripLeading();
            }
        }
        return lines;
    }

    private void drawScrollbar(GuiGraphicsExtractor g, int x, int top, int bottom, int lines, int visible) {
        g.fill(x, top, x + SCROLLBAR_WIDTH, bottom, SCROLL_TRACK);
        int track = bottom - top;
        int thumb = Math.max(6, track * visible / lines);
        int maxScroll = Math.max(1, lines - visible);
        int offset = (track - thumb) * Math.min(maxScroll, Math.max(0, scroll)) / maxScroll;
        g.fill(x, top + offset, x + SCROLLBAR_WIDTH, top + offset + thumb, SCROLL_THUMB);
    }

    /**
     * Wheel handling, mirroring {@link CompatibilityPanel#scrolled}: consumes the event only while the
     * cursor is over the panel and there is something to scroll to.
     */
    public boolean scrolled(double mouseX, double mouseY, double scrollY) {
        if (mouseX < lastX0 || mouseX >= lastX1 || mouseY < lastY0 || mouseY >= lastY1) return false;
        if (lastLines <= lastVisible) return false;
        scroll = SeedCompatibility.clampScroll(scroll - (int) Math.signum(scrollY), lastLines, lastVisible);
        return true;
    }
}
