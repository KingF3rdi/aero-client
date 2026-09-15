package dev.aero.client.ui;

import dev.aero.client.AeroClient;
import dev.aero.client.config.ClientConfig;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/** Save the current settings as a named profile, or switch to / delete a previously saved one. */
public class ProfilesScreen extends Screen {
    private static final int BG = 0xCC09080F;
    private static final int PANEL = 0xF00E0C16;
    private static final int TEXT = 0xFFF3F0F8;
    private static final int MUTED = 0xFF8E889C;
    private static final int ACCENT = 0xFFC4B5FD;

    private final Screen parent;
    private String draft = "";
    private boolean draftFocus;
    private List<String> profiles;

    public ProfilesScreen(Screen parent) {
        super(Text.literal("Profiles"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        profiles = ClientConfig.listProfiles();
    }

    private int panelX() {
        return width / 2 - 140;
    }

    private int panelY() {
        return height / 2 - 130;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, width, height, BG);
        int x = panelX();
        int y = panelY();
        int w = 280;
        int h = 260;
        UiDraw.card(context, x, y, w, h, PANEL, true);
        context.drawText(textRenderer, Text.literal("Profiles"), x + 14, y + 12, TEXT, false);
        context.drawText(textRenderer, Text.literal("Save the current setup, or switch to a saved one."),
                x + 14, y + 26, MUTED, false);

        UiDraw.inset(context, x + 14, y + 44, w - 96, 20, draftFocus ? 0xFF16141F : 0xCC12111A);
        String shown = draft.isEmpty() && !draftFocus ? "New profile name" : draft + (draftFocus ? "|" : "");
        context.drawText(textRenderer, Text.literal(shown), x + 20, y + 50,
                draft.isEmpty() && !draftFocus ? MUTED : TEXT, false);
        boolean saveHover = inside(mouseX, mouseY, x + w - 76, y + 44, 62, 20);
        UiDraw.pill(context, x + w - 76, y + 44, 62, 20, saveHover);
        context.drawText(textRenderer, Text.literal("Save"), x + w - 76 + 18, y + 50, saveHover ? TEXT : MUTED, false);

        int ry = y + 78;
        if (profiles.isEmpty()) {
            context.drawText(textRenderer, Text.literal("No saved profiles yet."), x + 14, ry, MUTED, false);
        }
        for (String name : profiles) {
            boolean rowHover = inside(mouseX, mouseY, x + 14, ry, w - 28, 20);
            if (rowHover) {
                context.fill(x + 12, ry - 2, x + w - 12, ry + 18, 0xFF14121C);
            }
            context.drawText(textRenderer, Text.literal(name), x + 18, ry + 4, TEXT, false);
            boolean delHover = inside(mouseX, mouseY, x + w - 30, ry, 18, 18);
            context.drawText(textRenderer, Text.literal("x"), x + w - 24, ry + 4, delHover ? 0xFFE05555 : MUTED, false);
            ry += 24;
        }

        boolean backHover = inside(mouseX, mouseY, x + 14, y + h - 30, 60, 20);
        UiDraw.pill(context, x + 14, y + h - 30, 60, 20, backHover);
        context.drawText(textRenderer, Text.literal("Back"), x + 14 + 14, y + h - 24, backHover ? TEXT : MUTED, false);
    }

    private static boolean inside(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && my >= y && mx < x + w && my < y + h;
    }

    private boolean click(int mx, int my) {
        int x = panelX();
        int y = panelY();
        int w = 280;
        int h = 260;

        if (inside(mx, my, x + 14, y + 44, w - 96, 20)) {
            draftFocus = true;
            return true;
        }
        if (inside(mx, my, x + w - 76, y + 44, 62, 20)) {
            if (!draft.isBlank()) {
                AeroClient.CONFIG.saveAsProfile(draft);
                draft = "";
                draftFocus = false;
                profiles = ClientConfig.listProfiles();
            }
            return true;
        }

        int ry = y + 78;
        for (String name : profiles) {
            if (inside(mx, my, x + w - 30, ry, 18, 18)) {
                ClientConfig.deleteProfile(name);
                profiles = ClientConfig.listProfiles();
                return true;
            }
            if (inside(mx, my, x + 14, ry, w - 28, 20)) {
                ClientConfig loaded = ClientConfig.loadProfile(name);
                if (loaded != null) {
                    AeroClient.switchConfig(loaded);
                }
                return true;
            }
            ry += 24;
        }

        if (inside(mx, my, x + 14, y + h - 30, 60, 20)) {
            client.setScreen(parent);
            return true;
        }

        draftFocus = false;
        return false;
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        return click((int) click.x(), (int) click.y());
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        int key = input.key();
        if (draftFocus) {
            if (key == GLFW.GLFW_KEY_BACKSPACE && !draft.isEmpty()) {
                draft = draft.substring(0, draft.length() - 1);
                return true;
            }
            if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
                if (!draft.isBlank()) {
                    AeroClient.CONFIG.saveAsProfile(draft);
                    draft = "";
                    draftFocus = false;
                    profiles = ClientConfig.listProfiles();
                }
                return true;
            }
            if (key == GLFW.GLFW_KEY_ESCAPE) {
                draftFocus = false;
                return true;
            }
            return true;
        }
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            client.setScreen(parent);
            return true;
        }
        return false;
    }

    @Override
    public boolean charTyped(CharInput input) {
        int cp = input.codepoint();
        if (draftFocus && cp >= 32 && cp != 127 && draft.length() < 32) {
            draft += Character.toString(cp);
            return true;
        }
        return false;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
