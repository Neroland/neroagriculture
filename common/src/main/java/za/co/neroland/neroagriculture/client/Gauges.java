package za.co.neroland.neroagriculture.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/**
 * Shared labelled gauge for the texture-free screens: a 10px bar with its caption drawn inside
 * ("Energy", "Nutrient", "Progress", ...), so every bar in every machine UI is self-explanatory.
 */
public final class Gauges {
    public static final int ENERGY = 0xFFE0B33A;    // amber — NeroFlux
    public static final int NUTRIENT = 0xFF42A880;  // teal — nutrient solution
    public static final int PROGRESS = 0xFF78C860;  // bio-green — work progress
    private static final int TROUGH = 0xFF2A343C;
    private static final int LABEL = 0xFFE7EEF3;

    private Gauges() { }

    /** Draw a labelled bar from {@code (x0,y0)} to {@code (x1,y0+10)} filled by {@code value/max}. */
    public static void bar(GuiGraphicsExtractor g, Font font, int x0, int y0, int x1,
            String label, int value, int max, int fillColor) {
        g.fill(x0, y0, x1, y0 + 10, TROUGH);
        int width = x1 - x0 - 2;
        int fill = max <= 0 ? 0 : (int) Math.min(width, (long) Math.max(0, value) * width / max);
        if (fill > 0) g.fill(x0 + 1, y0 + 1, x0 + 1 + fill, y0 + 9, fillColor);
        g.text(font, Component.literal(label), x0 + 3, y0 + 1, LABEL, false);
    }
}
