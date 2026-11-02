package dev.aero.client.ui;

import dev.aero.client.AeroClient;

/** Menu themes (the dark base the cards and panels are tinted with) and the accent palettes of the Settings page. */
public final class Themes {
    /** solid: how much denser the panels are (OLED is near black). */
    public record Theme(String name, int base, int accent, float solid) {}

    public static final Theme[] THEMES = {
            new Theme("Aero", 0x0C0D12, 0x4F8EFF, 0f),
            new Theme("Aurora", 0x130F24, 0x8B7CF6, 0f),
            new Theme("Midnight", 0x0A1228, 0x4F8EFF, 0f),
            new Theme("OLED", 0x000000, 0xF5F5F7, 0.15f),
            new Theme("Ender", 0x160B26, 0xC084FC, 0f),
            new Theme("Nether", 0x230B0B, 0xF8604A, 0f),
            new Theme("Deep Dark", 0x05181C, 0x22D3EE, 0f),
            new Theme("Cherry Grove", 0x240E1B, 0xF77FB4, 0f),
    };

    public record Accent(String name, int a, int b) {}

    public static final Accent[] SOLID = {
            new Accent("Violet", 0x8B7CF6, 0), new Accent("Blue", 0x4F8EFF, 0), new Accent("Sky", 0x38BDF8, 0),
            new Accent("Teal", 0x2DD4BF, 0), new Accent("Green", 0x34D399, 0), new Accent("Lime", 0xA3E635, 0),
            new Accent("Amber", 0xFBBF24, 0), new Accent("Orange", 0xFB923C, 0), new Accent("Red", 0xF05252, 0),
            new Accent("Pink", 0xFB7199, 0), new Accent("Magenta", 0xE879F9, 0), new Accent("White", 0xF5F5F7, 0),
    };

    public static final Accent[] GRADIENT = {
            new Accent("Lagoon", 0x67E8F9, 0xC4B5FD), new Accent("Peach", 0xFDBA74, 0xF9A8D4), new Accent("Ocean", 0x38BDF8, 0x2563EB),
            new Accent("Dusk", 0x818CF8, 0xE879F9), new Accent("Sunset", 0xFB923C, 0xEF4444), new Accent("Mint", 0x2DD4BF, 0x86EFAC),
            new Accent("Orchid", 0xF0ABFC, 0xC4B5FD), new Accent("Abyss", 0x22D3EE, 0x6366F1), new Accent("Coral", 0xFB7185, 0xFDBA74),
            new Accent("Meadow", 0xA3E635, 0x22C55E), new Accent("Glacier", 0x93C5FD, 0x60A5FA), new Accent("Pearl", 0xF5F5F7, 0xC7D2FE),
    };

    private Themes() {}

    public static Theme current() {
        var c = AeroClient.CONFIG;
        String n = c == null ? null : c.uiTheme;
        for (Theme t : THEMES) {
            if (t.name().equalsIgnoreCase(n)) {
                return t;
            }
        }
        return THEMES[0];
    }

    /** The theme's dark base color (rgb). */
    public static int base() {
        return current().base();
    }
}
