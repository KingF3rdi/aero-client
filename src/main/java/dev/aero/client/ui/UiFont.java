package dev.aero.client.ui;

import dev.aero.client.AeroClient;
import net.minecraft.text.OrderedText;
import net.minecraft.text.StyleSpriteSource;
import net.minecraft.util.Identifier;

/**
 * Smooth menu font (Inter, assets/aero/font/ui.json). While an Aero screen renders, text in the
 * default font is measured and drawn with it instead - no change to the hundreds of draw calls.
 * GUI text is drawn later than it is submitted, so submitted text is pinned to the font right away.
 */
public final class UiFont {
    private static final StyleSpriteSource.Font FONT = new StyleSpriteSource.Font(Identifier.of("aero", "ui"));
    public static boolean active;

    private UiFont() {}

    public static boolean enabled() {
        return AeroClient.CONFIG == null || AeroClient.CONFIG.smoothFont;
    }

    /** Centered text in the menu font, for Aero-styled vanilla widgets. */
    public static void centered(net.minecraft.client.gui.DrawContext ctx, net.minecraft.client.font.TextRenderer tr,
                                net.minecraft.text.Text text, int x, int y, int color) {
        boolean was = active;
        active = was || enabled();
        try {
            ctx.drawCenteredTextWithShadow(tr, text, x, y, color);
        } finally {
            active = was;
        }
    }

    public static StyleSpriteSource map(StyleSpriteSource source) {
        return active && StyleSpriteSource.DEFAULT.equals(source) ? FONT : source;
    }

    public static OrderedText pin(OrderedText text) {
        if (!active) {
            return text;
        }
        return visitor -> text.accept((index, style, codePoint) ->
                visitor.accept(index, StyleSpriteSource.DEFAULT.equals(style.getFont()) ? style.withFont(FONT) : style, codePoint));
    }
}
