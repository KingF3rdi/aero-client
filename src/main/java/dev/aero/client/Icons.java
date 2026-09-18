package dev.aero.client;

import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.StyleSpriteSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/** Item-texture glyphs (see assets/aero/font/icons.json) usable inside any Text: nametags, tab list, chat. */
public final class Icons {
    public static final Identifier FONT = Identifier.of("aero", "icons");

    private Icons() {}

    private static MutableText glyph(char c) {
        return Text.literal(String.valueOf(c)).setStyle(Style.EMPTY.withFont(new StyleSpriteSource.Font(FONT)));
    }

    public static MutableText totem() {
        return glyph('');
    }

    public static MutableText mode(String gamemode) {
        String gm = gamemode == null ? "vanilla" : gamemode.toLowerCase(java.util.Locale.ROOT);
        return glyph(switch (gm) {
            case "uhc" -> '';
            case "pot" -> '';
            case "nethop" -> '';
            case "smp" -> '';
            case "sword" -> '';
            case "axe" -> '';
            case "mace" -> '';
            default -> '';
        });
    }

    /** MCTiers-style colors: HT brighter than LT, tier 1 gold down to tier 5 grey. */
    public static int tierColor(String tier) {
        boolean high = tier.contains("HT");
        int n = 5;
        for (int i = tier.length() - 1; i >= 0; i--) {
            if (Character.isDigit(tier.charAt(i))) {
                n = tier.charAt(i) - '0';
                break;
            }
        }
        int[] highCols = {0xFFC94D, 0xFF9F43, 0xFF6B6B, 0xC77DFF, 0x9AA5B1};
        int[] lowCols = {0xE8C86A, 0xE08A3A, 0xD05A5A, 0xA060D8, 0x7C8794};
        int idx = Math.max(1, Math.min(5, n)) - 1;
        return high ? highCols[idx] : lowCols[idx];
    }
}
