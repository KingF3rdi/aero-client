package dev.aero.client.ui;

import dev.aero.client.config.ForeignConfigs;
import dev.aero.client.cosmetic.CosmeticPreview;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerWarningScreen;
import net.minecraft.client.gui.screen.option.CreditsAndAttributionScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.text.Text;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Aero title screen (replaces vanilla's while "Aero title screen" is on): logo, What's new on the left,
 * the main buttons in the middle, your player with the Wardrobe button on the right, the Sky switch top right.
 * Laid out in a 640x360 design space scaled to the window.
 */
public class AeroTitleScreen extends Screen {
    private static final int TEXT = UiDraw.TEXT;
    private static final int MUTED = UiDraw.MUTED;
    private static final int BW = 200;
    private static final int BH = 22;
    private static final boolean MOD_MENU = FabricLoader.getInstance().isModLoaded("modmenu");
    private static List<String> changelog;

    private final long openedAt = System.currentTimeMillis();
    private final float[] hover = new float[16];
    private long lastFrame;

    /** One clickable thing: id, rect, label. */
    private record Btn(int id, int x, int y, int w, int h, String label) {
        boolean hit(int mx, int my) {
            return mx >= x && my >= y && mx < x + w && my < y + h;
        }
    }

    public AeroTitleScreen() {
        super(Text.literal("Aero"));
    }

    private float scale() {
        return Math.max(0.5f, Math.min(width / 640f, height / 360f));
    }

    private int vw() {
        return Math.round(width / scale());
    }

    private int vh() {
        return Math.round(height / scale());
    }

    private int buttonsTop() {
        return Math.round(vh() * 0.38f);
    }

    private List<Btn> buttons() {
        List<Btn> out = new ArrayList<>();
        int x = vw() / 2 - BW / 2;
        int y = buttonsTop();
        String[] main = MOD_MENU ? new String[]{"Singleplayer", "Multiplayer", "Aero", "Mods"} : new String[]{"Singleplayer", "Multiplayer", "Aero"};
        for (int i = 0; i < main.length; i++) {
            out.add(new Btn(i, x, y, BW, BH, main[i]));
            y += BH + 6;
        }
        y += 8;
        int half = (BW - 8) / 2;
        out.add(new Btn(4, x, y, half, BH, "Options"));
        out.add(new Btn(5, x + half + 8, y, half, BH, "Quit Game"));
        // right column: Wardrobe under the player, Sky top right
        int rx = modelX();
        out.add(new Btn(6, rx - 50, buttonsTop() + 132, 100, 20, "Wardrobe"));
        out.add(new Btn(7, vw() - 86, 8, 78, 18, "Sky: " + TitleSky.current()));
        return out;
    }

    private int modelX() {
        return Math.round(vw() * 0.82f);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta); // background: the chosen sky or vanilla's panorama
        float s = scale();
        int mx = Math.round(mouseX / s);
        int my = Math.round(mouseY / s);
        long now = System.nanoTime();
        float k = lastFrame == 0 ? 1f : 1f - (float) Math.exp(-(now - lastFrame) / 1e9 * 14);
        lastFrame = now;
        float in = Math.min(1f, (System.currentTimeMillis() - openedAt) / 350f);
        UiDraw.fade = 1f - (1f - in) * (1f - in);
        context.getMatrices().pushMatrix();
        context.getMatrices().scale(s, s);
        try {
            drawScaled(context, mx, my, k);
        } finally {
            context.getMatrices().popMatrix();
            UiDraw.fade = 1f;
        }
    }

    private void drawScaled(DrawContext c, int mx, int my, float k) {
        int vw = vw();
        int vh = vh();
        int text = UiDraw.fa(TEXT);
        int muted = UiDraw.fa(MUTED);
        boolean visible = (text >>> 24) >= 8;

        // logo
        String name = "Aero";
        float big = 3.4f;
        int tw = Math.round(textRenderer.getWidth(name) * big);
        int markW = Math.round(9 * big);
        int tx = (vw - tw - markW - 10) / 2;
        int ty = Math.round(vh * 0.15f);
        UiDraw.aeroMark(c, tx, ty - 2, markW, UiDraw.fa(UiDraw.accent()));
        if (visible) {
            c.getMatrices().pushMatrix();
            c.getMatrices().translate(tx + markW + 10, ty);
            c.getMatrices().scale(big, big);
            c.drawText(textRenderer, Text.literal(name), 0, 0, text, true);
            c.getMatrices().popMatrix();
        }

        for (Btn b : buttons()) {
            boolean h = b.hit(mx, my);
            hover[b.id()] += ((h ? 1f : 0f) - hover[b.id()]) * k;
            UiDraw.frost(c, b.x(), b.y(), b.w(), b.h(), Math.min(10, b.h() / 2), hover[b.id()], false);
            if (visible) {
                c.drawText(textRenderer, Text.literal(b.label()), b.x() + (b.w() - textRenderer.getWidth(b.label())) / 2,
                        b.y() + (b.h() - 8) / 2, text, false);
            }
        }
        if (!visible) {
            return;
        }

        drawAccount(c, text);
        drawChangelog(c, text, muted);
        drawPlayer(c, mx, text, muted);

        String ver = "Aero Client   Minecraft " + version("minecraft"); // the jar's own version is not the release number
        int foot = UiDraw.fa(0xD0FFFFFF); // shadowed: the skies get bright at the bottom
        c.drawText(textRenderer, Text.literal(ver), 6, vh - 12, foot, true);
        String copy = "Copyright Mojang AB. Do not distribute!";
        c.drawText(textRenderer, Text.literal(copy), vw - 6 - textRenderer.getWidth(copy), vh - 12, foot, true);
    }

    /** Top left: your head and name. */
    private void drawAccount(DrawContext c, int text) {
        String nm = client.getSession().getUsername();
        int w = 12 + 6 + textRenderer.getWidth(nm) + 16;
        UiDraw.frost(c, 8, 8, w, 18, 9, 0f, false);
        try {
            var skin = client.getSkinProvider().supplySkinTextures(client.getGameProfile(), false).get();
            PlayerSkinDrawer.draw(c, skin, 13, 11, 12);
        } catch (Throwable ignored) {
        }
        c.drawText(textRenderer, Text.literal(nm), 31, 13, text, false);
    }

    private void drawChangelog(DrawContext c, int text, int muted) {
        int x = Math.round(vw() * 0.07f);
        int maxW = Math.min(170, vw() / 2 - BW / 2 - 24 - x);
        if (maxW < 80) {
            return; // window too narrow
        }
        int y = buttonsTop();
        c.drawText(textRenderer, Text.literal("What's new"), x, y, text, false);
        y += 12;
        List<String> lines = new ArrayList<>(changelog());
        if (!ForeignConfigs.IMPORTED.isEmpty()) {
            int n = ForeignConfigs.IMPORTED.size();
            lines.add(0, "• Took over your settings from " + n + (n == 1 ? " mod" : " mods"));
        }
        int bottom = buttonsTop() + 150;
        boolean first = true;
        for (String line : lines) {
            boolean head = !line.startsWith("•");
            if (head && !first) {
                y += 6;
            }
            first = false;
            for (var row : textRenderer.wrapLines(Text.literal(line), maxW)) {
                if (y > bottom) {
                    return;
                }
                c.drawText(textRenderer, row, x, y, head ? text : muted, false);
                y += 10;
            }
            if (head) {
                y += 2;
            }
        }
    }

    /** Right: "● Beta | name" above your player, who turns toward the mouse. */
    private void drawPlayer(DrawContext c, int mx, int text, int muted) {
        int cx = modelX();
        int top = buttonsTop();
        String nm = client.getSession().getUsername();
        String tag = "Beta";
        int w = 8 + 6 + textRenderer.getWidth(tag) + 12 + textRenderer.getWidth(nm) + 10;
        int px = cx - w / 2;
        int py = top - 22;
        UiDraw.frost(c, px, py, w, 16, 8, 0f, false);
        UiDraw.roundRect(c, px + 7, py + 6, 4, 4, 2, UiDraw.fa(UiDraw.accent()));
        c.drawText(textRenderer, Text.literal(tag), px + 15, py + 4, text, false);
        int sep = px + 15 + textRenderer.getWidth(tag) + 6;
        c.fill(sep, py + 3, sep + 1, py + 13, UiDraw.fa(0x40FFFFFF));
        c.drawText(textRenderer, Text.literal(nm), sep + 6, py + 4, text, false);
        float yaw = 180f - Math.max(-45f, Math.min(45f, (mx - cx) * 0.18f));
        CosmeticPreview.showSelf(c, cx - 50, top - 2, cx + 50, top + 126, 58f, yaw);
    }

    private static List<String> changelog() {
        if (changelog == null) {
            changelog = new ArrayList<>();
            try (var in = AeroTitleScreen.class.getResourceAsStream("/assets/aero/changelog.txt")) {
                if (in != null) {
                    new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8)).lines()
                            .map(String::strip).filter(l -> !l.isEmpty()).forEach(changelog::add);
                }
            } catch (Exception ignored) {
            }
        }
        return changelog;
    }

    private static String version(String mod) {
        return FabricLoader.getInstance().getModContainer(mod).map(m -> m.getMetadata().getVersion().getFriendlyString()).orElse("?");
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        float s = scale();
        int mx = Math.round((float) (click.x() / s));
        int my = Math.round((float) (click.y() / s));
        for (Btn b : buttons()) {
            if (b.hit(mx, my)) {
                press(b.id());
                return true;
            }
        }
        String copy = "Copyright Mojang AB. Do not distribute!";
        if (my >= vh() - 14 && mx >= vw() - 6 - textRenderer.getWidth(copy)) {
            client.setScreen(new CreditsAndAttributionScreen(this));
            return true;
        }
        return super.mouseClicked(click, doubled);
    }

    private void press(int id) {
        switch (id) {
            case 0 -> client.setScreen(new SelectWorldScreen(this));
            case 1 -> client.setScreen(client.options.skipMultiplayerWarning ? new MultiplayerScreen(this) : new MultiplayerWarningScreen(this));
            case 2 -> client.setScreen(Menus.clickGui(this, false));
            case 3 -> {
                try {
                    client.setScreen((Screen) Class.forName("com.terraformersmc.modmenu.gui.ModsScreen").getConstructor(Screen.class).newInstance(this));
                } catch (ReflectiveOperationException ignored) {
                }
            }
            case 4 -> client.setScreen(new OptionsScreen(this, client.options));
            case 5 -> client.scheduleStop();
            case 6 -> {
                Screen gui = Menus.clickGui(this, false);
                if (gui instanceof ClickGuiScreenLegacy g) {
                    g.debugTab(1, 0);
                }
                client.setScreen(gui);
            }
            default -> TitleSky.cycle();
        }
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }
}
