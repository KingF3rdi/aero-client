package dev.aero.client.ui;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.aero.client.AeroClient;
import dev.aero.client.config.ClientConfig;
import dev.aero.client.social.AeroApi;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Profiles: Saved (setups on this PC) and Public (setups other Aero players shared on the Aero server).
 * Loading a public one keeps everything personal (server, cosmetics, Discord) from your own config.
 */
public class ProfilesScreen extends Screen {
    private static final int TEXT = UiDraw.TEXT;
    private static final int MUTED = UiDraw.MUTED;
    private static final int PW = 320;
    private static final int PH = 280;
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(6)).build();

    private record Preset(int id, String name, String owner, String ownerUuid) {}

    private final Screen parent;
    private String draft = "";
    private boolean draftFocus;
    private List<String> profiles;
    private int tab;
    private volatile List<Preset> presets;
    private volatile String status = "";
    private int scroll;
    private float s = 1f;

    public ProfilesScreen(Screen parent) {
        super(Text.literal("Profiles"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        profiles = ClientConfig.listProfiles();
    }

    private float scale() {
        return Math.max(0.6f, Math.min(2.5f, Math.min(width * 0.6f / PW, height * 0.8f / PH)));
    }

    private int px() {
        return Math.round(width / s) / 2 - PW / 2;
    }

    private int py() {
        return Math.round(height / s) / 2 - PH / 2;
    }

    private void fetchPresets() {
        status = "Loading…";
        String base = AeroApi.base();
        Thread t = new Thread(() -> {
            try {
                HttpResponse<String> r = HTTP.send(HttpRequest.newBuilder(URI.create(base + "/api/presets"))
                        .timeout(Duration.ofSeconds(8)).GET().build(), HttpResponse.BodyHandlers.ofString());
                List<Preset> out = new ArrayList<>();
                for (JsonElement e : JsonParser.parseString(r.body()).getAsJsonObject().getAsJsonArray("presets")) {
                    JsonObject o = e.getAsJsonObject();
                    out.add(new Preset(o.get("id").getAsInt(), o.get("name").getAsString(), o.get("owner").getAsString(),
                            o.get("ownerUuid").getAsString()));
                }
                presets = out;
                status = out.isEmpty() ? "Nobody shared a profile yet." : "";
            } catch (Exception e) {
                status = "Aero server not reachable";
            }
        }, "aero-presets");
        t.setDaemon(true);
        t.start();
    }

    private void loadPreset(Preset p) {
        status = "Loading " + p.name() + "…";
        String base = AeroApi.base();
        Thread t = new Thread(() -> {
            try {
                HttpResponse<String> r = HTTP.send(HttpRequest.newBuilder(URI.create(base + "/api/presets/" + p.id() + ".json"))
                        .timeout(Duration.ofSeconds(8)).GET().build(), HttpResponse.BodyHandlers.ofString());
                client.execute(() -> {
                    ClientConfig cfg = r.statusCode() == 200 ? AeroClient.CONFIG.withPublicPreset(r.body()) : null;
                    if (cfg != null) {
                        AeroClient.switchConfig(cfg);
                        status = "Loaded " + p.name() + " by " + p.owner();
                    } else {
                        status = "Couldn't load that profile";
                    }
                });
            } catch (Exception e) {
                status = "Aero server not reachable";
            }
        }, "aero-presets");
        t.setDaemon(true);
        t.start();
    }

    private void publish() {
        String token = AeroApi.authToken();
        if (draft.isBlank()) {
            status = "Type a name first";
            return;
        }
        if (token == null) {
            status = "Not connected to the Aero server yet";
            return;
        }
        String name = draft.trim();
        String body = AeroClient.CONFIG.publicJson();
        String base = AeroApi.base();
        status = "Sharing…";
        Thread t = new Thread(() -> {
            try {
                HttpResponse<String> r = HTTP.send(HttpRequest.newBuilder(URI.create(base + "/api/presets?name="
                                + URLEncoder.encode(name, StandardCharsets.UTF_8))).timeout(Duration.ofSeconds(8))
                        .header("authorization", "Bearer " + token).header("content-type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
                if (r.statusCode() == 200) {
                    status = "Shared as " + name;
                    draft = "";
                    fetchPresets();
                } else {
                    status = JsonParser.parseString(r.body()).getAsJsonObject().get("error").getAsString();
                }
            } catch (Exception e) {
                status = "Aero server not reachable";
            }
        }, "aero-presets");
        t.setDaemon(true);
        t.start();
    }

    private void deletePreset(Preset p) {
        String token = AeroApi.authToken();
        if (token == null) {
            return;
        }
        String base = AeroApi.base();
        Thread t = new Thread(() -> {
            try {
                HTTP.send(HttpRequest.newBuilder(URI.create(base + "/api/presets/" + p.id())).timeout(Duration.ofSeconds(8))
                        .header("authorization", "Bearer " + token).DELETE().build(), HttpResponse.BodyHandlers.discarding());
                fetchPresets();
            } catch (Exception ignored) {
            }
        }, "aero-presets");
        t.setDaemon(true);
        t.start();
    }

    private boolean mine(Preset p) {
        var mc = client;
        return mc != null && mc.getSession().getUuidOrNull() != null
                && p.ownerUuid().equalsIgnoreCase(mc.getSession().getUuidOrNull().toString());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        s = scale();
        context.fill(0, 0, width, height, 0x40E8EDF5);
        int mx = (int) (mouseX / s);
        int my = (int) (mouseY / s);
        context.getMatrices().pushMatrix();
        context.getMatrices().scale(s, s);
        try {
            renderScaled(context, mx, my);
        } finally {
            context.getMatrices().popMatrix();
        }
    }

    private void renderScaled(DrawContext context, int mx, int my) {
        int x = px();
        int y = py();
        UiDraw.glass(context, x, y, PW, PH, 0, 18);
        context.drawText(textRenderer, Text.literal("Profiles"), x + 16, y + 14, TEXT, false);
        String[] tabs = {"Saved", "Public"};
        for (int i = 0; i < 2; i++) {
            int tx = x + PW - 16 - (2 - i) * 64;
            boolean on = tab == i;
            UiDraw.pill(context, tx, y + 10, 60, 18, on || inside(mx, my, tx, y + 10, 60, 18));
            context.drawText(textRenderer, Text.literal(tabs[i]), tx + (60 - textRenderer.getWidth(tabs[i])) / 2, y + 15, on ? TEXT : MUTED, false);
        }
        context.drawText(textRenderer, Text.literal(tab == 0 ? "Setups saved on this PC." : "Setups other Aero players shared."),
                x + 16, y + 30, MUTED, false);

        UiDraw.field(context, x + 14, y + 46, PW - 104, 20, draftFocus);
        String shown = draft.isEmpty() && !draftFocus ? (tab == 0 ? "New profile name" : "Share yours as…") : draft + (draftFocus ? "|" : "");
        context.drawText(textRenderer, Text.literal(shown), x + 20, y + 52, draft.isEmpty() && !draftFocus ? MUTED : TEXT, false);
        boolean saveHover = inside(mx, my, x + PW - 84, y + 46, 70, 20);
        UiDraw.roundRect(context, x + PW - 84, y + 46, 70, 20, 10, saveHover ? UiDraw.accent() : UiDraw.withAlpha(UiDraw.accent(), 0xDD));
        String bl = tab == 0 ? "Save" : "Share";
        context.drawText(textRenderer, Text.literal(bl), x + PW - 84 + (70 - textRenderer.getWidth(bl)) / 2, y + 52, 0xFFFFFFFF, false);

        int top = y + 76;
        int bottom = y + PH - 40;
        context.enableScissor(x + 8, top, x + PW - 8, bottom);
        try {
            int ry = top - scroll;
            if (tab == 0) {
                if (profiles.isEmpty()) {
                    context.drawText(textRenderer, Text.literal("No saved profiles yet."), x + 16, ry + 4, MUTED, false);
                }
                for (String name : profiles) {
                    boolean rowHover = inside(mx, my, x + 12, ry, PW - 24, 22) && my >= top && my < bottom;
                    UiDraw.roundRect(context, x + 12, ry, PW - 24, 22, 10, rowHover ? UiDraw.HOVER_FILL : UiDraw.LIFT);
                    context.drawText(textRenderer, Text.literal(name), x + 20, ry + 7, TEXT, false);
                    boolean delHover = inside(mx, my, x + PW - 34, ry + 2, 18, 18);
                    context.drawText(textRenderer, Text.literal("×"), x + PW - 28, ry + 7, delHover ? 0xFFDC2626 : MUTED, false);
                    ry += 26;
                }
            } else {
                List<Preset> list = presets;
                if (list != null) {
                    for (Preset p : list) {
                        boolean rowHover = inside(mx, my, x + 12, ry, PW - 24, 26) && my >= top && my < bottom;
                        UiDraw.roundRect(context, x + 12, ry, PW - 24, 26, 10, rowHover ? UiDraw.HOVER_FILL : UiDraw.LIFT);
                        context.drawText(textRenderer, Text.literal(p.name()), x + 20, ry + 4, TEXT, false);
                        context.drawText(textRenderer, Text.literal("by " + p.owner()), x + 20, ry + 14, MUTED, false);
                        if (mine(p)) {
                            boolean delHover = inside(mx, my, x + PW - 34, ry + 4, 18, 18);
                            context.drawText(textRenderer, Text.literal("×"), x + PW - 28, ry + 9, delHover ? 0xFFDC2626 : MUTED, false);
                        } else if (rowHover) {
                            context.drawText(textRenderer, Text.literal("Load"), x + PW - 20 - textRenderer.getWidth("Load"), ry + 9, UiDraw.accent(), false);
                        }
                        ry += 30;
                    }
                }
            }
        } finally {
            context.disableScissor();
        }
        if (!status.isEmpty()) {
            context.drawText(textRenderer, Text.literal(status), x + 90, y + PH - 24, MUTED, false);
        }
        boolean backHover = inside(mx, my, x + 14, y + PH - 30, 64, 20);
        UiDraw.pill(context, x + 14, y + PH - 30, 64, 20, backHover);
        context.drawText(textRenderer, Text.literal("‹ Back"), x + 14 + (64 - textRenderer.getWidth("‹ Back")) / 2, y + PH - 24, TEXT, false);
    }

    private static boolean inside(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && my >= y && mx < x + w && my < y + h;
    }

    private void save() {
        if (draft.isBlank()) {
            return;
        }
        if (tab == 1) {
            publish();
            return;
        }
        AeroClient.CONFIG.saveAsProfile(draft);
        draft = "";
        draftFocus = false;
        profiles = ClientConfig.listProfiles();
        status = "Saved";
    }

    private boolean click(int mx, int my) {
        int x = px();
        int y = py();
        for (int i = 0; i < 2; i++) {
            if (inside(mx, my, x + PW - 16 - (2 - i) * 64, y + 10, 60, 18)) {
                tab = i;
                scroll = 0;
                status = "";
                if (i == 1) {
                    fetchPresets();
                }
                return true;
            }
        }
        if (inside(mx, my, x + 14, y + 46, PW - 104, 20)) {
            draftFocus = true;
            return true;
        }
        if (inside(mx, my, x + PW - 84, y + 46, 70, 20)) {
            save();
            return true;
        }
        int top = y + 76;
        int bottom = y + PH - 40;
        if (my >= top && my < bottom) {
            int ry = top - scroll;
            if (tab == 0) {
                for (String name : profiles) {
                    if (inside(mx, my, x + PW - 34, ry + 2, 18, 18)) {
                        ClientConfig.deleteProfile(name);
                        profiles = ClientConfig.listProfiles();
                        return true;
                    }
                    if (inside(mx, my, x + 12, ry, PW - 24, 22)) {
                        ClientConfig loaded = ClientConfig.loadProfile(name);
                        if (loaded != null) {
                            AeroClient.switchConfig(loaded);
                            status = "Switched to " + name;
                        }
                        return true;
                    }
                    ry += 26;
                }
            } else if (presets != null) {
                for (Preset p : presets) {
                    if (mine(p) && inside(mx, my, x + PW - 34, ry + 4, 18, 18)) {
                        deletePreset(p);
                        return true;
                    }
                    if (inside(mx, my, x + 12, ry, PW - 24, 26)) {
                        loadPreset(p);
                        return true;
                    }
                    ry += 30;
                }
            }
        }
        if (inside(mx, my, x + 14, y + PH - 30, 64, 20)) {
            client.setScreen(parent);
            return true;
        }
        draftFocus = false;
        return false;
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        s = scale();
        return click((int) (click.x() / s), (int) (click.y() / s));
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        scroll = (int) Math.max(0, scroll - verticalAmount * 16);
        return true;
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
                save();
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
        if (draftFocus && cp >= 32 && cp != 127 && draft.length() < 24) {
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
